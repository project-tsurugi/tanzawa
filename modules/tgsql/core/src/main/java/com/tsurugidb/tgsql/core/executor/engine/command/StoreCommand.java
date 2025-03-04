/*
 * Copyright 2023-2025 Project Tsurugi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tsurugidb.tgsql.core.executor.engine.command;

import java.io.IOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.exception.TgsqlMessageException;
import com.tsurugidb.tgsql.core.executor.engine.BasicEngine;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.executor.result.type.BlobWrapper;
import com.tsurugidb.tgsql.core.executor.result.type.ClobWrapper;
import com.tsurugidb.tgsql.core.model.ErroneousStatement;
import com.tsurugidb.tgsql.core.model.ErroneousStatement.ErrorKind;
import com.tsurugidb.tgsql.core.model.Regioned;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Store command for Tsurugi SQL console.
 */
public class StoreCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(StoreCommand.class);

    private static final String COMMAND_NAME = "store"; //$NON-NLS-1$
    private static final String COMMAND = COMMAND_PREFIX + COMMAND_NAME;

    @FunctionalInterface
    private interface Executor {
        boolean execute(BasicEngine engine, StoreCommandArgument argument) throws EngineException, ServerException, IOException, InterruptedException;
    }

    private static class SubCommand {
        private final String name;
        private final boolean hasParameter;
        private final Executor executor;

        SubCommand(String name, boolean hasParameter, Executor executor) {
            this.name = name;
            this.hasParameter = hasParameter;
            this.executor = executor;
        }

        String name() {
            return this.name;
        }

        boolean hasParameter() {
            return this.hasParameter;
        }

        Executor executor() {
            return this.executor;
        }
    }

    private final List<SubCommand> subCommandList = new ArrayList<>();

    /**
     * Creates a new instance.
     */
    public StoreCommand() {
        super(COMMAND_NAME);

        add("blob", false, StoreCommand::executeStoreBlob); //$NON-NLS-1$
        add("clob", false, StoreCommand::executeStoreClob); //$NON-NLS-1$
    }

    private void add(String name, boolean hasParameter, Executor executor) {
        subCommandList.add(new SubCommand(toLowerCase(name), hasParameter, executor));
    }

    @Override
    protected void collectCompleterCandidate(List<CompleterCandidateWords> result) {
        for (var command : subCommandList) {
            result.add(new CompleterCandidateWords(COMMAND, command.name(), !command.hasParameter()));
        }
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        var list = findSubCommand(statement);
        if (list == null) {
            printHelp(engine);
            return true;
        }
        switch (list.size()) {
        case 0:
            return engine.execute(toSubUnknownError(statement));
        case 1:
            var command = list.get(0);
            var argument = parseArgument(command.name(), statement);
            return command.executor().execute(engine, argument);
        default:
            var nameList = list.stream().map(c -> c.name()).collect(Collectors.toList());
            return engine.execute(toSubUnknownError(statement, nameList));
        }
    }

    private @Nullable List<SubCommand> findSubCommand(SpecialStatement statement) {
        String option = getOption(statement, 0);
        if (option == null) {
            return null;
        }

        String s = toLowerCase(option);
        int n = s.indexOf('@');
        if (n >= 0) {
            s = s.substring(0, n).trim();
        }
        String subName = s;
        return subCommandList.stream().filter(command -> command.name().startsWith(subName)).collect(Collectors.toList());
    }

    static class StoreCommandArgument {
        public String objectPrefix;
        public int objectNumber;
        public String destination;
    }

    StoreCommandArgument parseArgument(String subName, SpecialStatement statement) {
        var argument = new StoreCommandArgument();

        int index = 0;
        String objectName = getOption(statement, index++);
        if (objectName != null && !objectName.contains("@")) {
            objectName = getOption(statement, index++);
        }
        if (objectName == null) {
            throw new TgsqlMessageException("objectName not specified");
        }

        String objectPrefix;
        String objectNumber;
        int n = objectName.indexOf('@');
        if (n >= 0) {
            objectPrefix = objectName.substring(0, n).trim();
            objectNumber = objectName.substring(n + 1).trim();
        } else {
            objectPrefix = subName;
            objectNumber = objectName;
            objectName = objectPrefix + "@" + objectNumber;
        }
        if (!subName.equals(objectPrefix)) {
            throw new TgsqlMessageException(MessageFormat.format("illegal objectName. target={0}, objectName={1}", subName, objectName));
        }
        argument.objectPrefix = objectPrefix;
        argument.objectNumber = toInt(subName, objectNumber);

        String destination = getOption(statement, index);
        if (destination == null) {
            throw new TgsqlMessageException("destination not specified");
        }
        argument.destination = destination;

        return argument;
    }

    private static int toInt(String prefix, String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new TgsqlMessageException(MessageFormat.format("not integer. objectNumber={0}", s), e);
        }
    }

    private static boolean executeStoreBlob(BasicEngine engine, StoreCommandArgument argument) throws EngineException, ServerException, IOException, InterruptedException {
        String objectName = argument.objectPrefix + "@" + argument.objectNumber;
        String destination = argument.destination;
        LOG.debug("store blob. objectName={}, destination={}", objectName, destination); //$NON-NLS-1$ $NON-NLS-2$

        var sqlProcessor = engine.getSqlProcessor();
        var transaction = sqlProcessor.getTransactionOrThrow();

        var blob = transaction.getObject(BlobWrapper.class, BlobWrapper.PREFIX, argument.objectNumber);
        blob.copyTo(transaction.getTransaction(), Path.of(destination));

        return true;
    }

    private static boolean executeStoreClob(BasicEngine engine, StoreCommandArgument argument) throws EngineException, ServerException, IOException, InterruptedException {
        String objectName = argument.objectPrefix + "@" + argument.objectNumber;
        String destination = argument.destination;
        LOG.debug("store clob. objectName={}, destination={}", objectName, destination); //$NON-NLS-1$ $NON-NLS-2$

        var sqlProcessor = engine.getSqlProcessor();
        var transaction = sqlProcessor.getTransactionOrThrow();

        var clob = transaction.getObject(ClobWrapper.class, ClobWrapper.PREFIX, argument.objectNumber);
        clob.copyTo(transaction.getTransaction(), Path.of(destination));

        return true;
    }

    private static ErroneousStatement toSubUnknownError(SpecialStatement statement) {
        Regioned<String> option = statement.getCommandOptions().get(0);
        return new ErroneousStatement(//
                statement.getText(), //
                statement.getRegion(), //
                ErrorKind.UNKNOWN_SPECIAL_COMMAND_OPTION, //
                option.getRegion(), //
                MessageFormat.format(//
                        "unrecognized sub option: \"{0}\"", //
                        option.getValue()));
    }

    private static ErroneousStatement toSubUnknownError(SpecialStatement statement, List<String> nameList) {
        Regioned<String> option = statement.getCommandOptions().get(0);
        return new ErroneousStatement(//
                statement.getText(), //
                statement.getRegion(), //
                ErrorKind.UNKNOWN_SPECIAL_COMMAND_OPTION, //
                option.getRegion(), //
                MessageFormat.format(//
                        "ambiguous sub option: \"{0}\" in {1}", //
                        option.getValue(), nameList));
    }
}
