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
        boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException;
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

        add("blob", true, StoreCommand::executeStoreBlob); //$NON-NLS-1$
        add("clob", true, StoreCommand::executeStoreClob); //$NON-NLS-1$
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
            var command = list.get(0).executor();
            return command.execute(engine, statement);
        default:
            var nameList = list.stream().map(c -> c.name()).collect(Collectors.toList());
            return engine.execute(toSubUnknownError(statement, nameList));
        }
    }

    @Nullable
    private List<SubCommand> findSubCommand(SpecialStatement statement) {
        String option = getOption(statement, 0);
        if (option == null) {
            return null;
        }

        String subName = toLowerCase(option);
        return subCommandList.stream().filter(command -> command.name().startsWith(subName)).collect(Collectors.toList());
    }

    private static boolean executeStoreBlob(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        String objectName = getOption(statement, 1);
        String destination = getOption(statement, 2);
        LOG.debug("store blob. objectName={}, destination={}", objectName, destination); //$NON-NLS-1$ $NON-NLS-2$

        var sqlProcessor = engine.getSqlProcessor();
        var transaction = sqlProcessor.getTransactionOrThrow();

        int id = parseId("blob", objectName);
        if (destination == null) {
            throw new TgsqlMessageException("destination not specified");
        }

        var blob = transaction.getObject(BlobWrapper.class, BlobWrapper.PREFIX, id);
        var client = sqlProcessor.getSqlClient();
        blob.copyTo(client, Path.of(destination));

        return true;
    }

    private static boolean executeStoreClob(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        String objectName = getOption(statement, 1);
        String destination = getOption(statement, 2);
        LOG.debug("store clob. objectName={}, destination={}", objectName, destination); //$NON-NLS-1$ $NON-NLS-2$

        var sqlProcessor = engine.getSqlProcessor();
        var transaction = sqlProcessor.getTransactionOrThrow();

        int id = parseId("clob", objectName);
        if (destination == null) {
            throw new TgsqlMessageException("destination not specified");
        }

        var clob = transaction.getObject(ClobWrapper.class, ClobWrapper.PREFIX, id);
        var client = sqlProcessor.getSqlClient();
        clob.copyTo(client, Path.of(destination));

        return true;
    }

    private static int parseId(String prefix, String objectName) {
        if (objectName == null) {
            throw new TgsqlMessageException("objectName not specified");
        }

        String s = objectName.trim();
        int n = objectName.indexOf('@');
        if (n >= 0) {
            if (!prefix.equalsIgnoreCase(objectName.substring(0, n).trim())) {
                throw new TgsqlMessageException(MessageFormat.format("not target object. target={0}, objectName={1}", prefix, objectName));
            }
            s = objectName.substring(n + 1).trim();
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new TgsqlMessageException(MessageFormat.format("not integer. objectName={0}", objectName), e);
        }
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
