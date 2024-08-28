/*
 * Copyright 2023-2024 Project Tsurugi.
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.executor.engine.BasicEngine;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.model.ErroneousStatement;
import com.tsurugidb.tgsql.core.model.ErroneousStatement.ErrorKind;
import com.tsurugidb.tgsql.core.model.Regioned;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Special command for Tsurugi SQL console.
 */
public abstract class SpecialCommand {

    /**
     * special command prefix.
     */
    public static final String COMMAND_PREFIX = "\\"; //$NON-NLS-1$

    private static final SpecialCommand[] COMMAND_LIST = { //
            new ConnectCommand(), //
            new DisconnectCommand(), //
            new ExitCommand(), //
            new HaltCommand(), //
            new HelpCommand(), //
            new HistoryCommand(), //
            new SetCommand(), //
            new ShowCommand(), //
            new StatusCommand(), //
            new TimingCommand(), //
    };
    static { // assertion
        var set = new HashSet<String>();
        for (var command : COMMAND_LIST) {
            for (var name : command.getCommandNameList()) {
                if (set.contains(name)) {
                    throw new AssertionError(name);
                }
                set.add(name);
            }
        }
    }

    /**
     * name and command pair.
     */
    public static class NameCommandPair {
        private final String name;
        private final SpecialCommand command;

        /**
         * Creates a new instance.
         *
         * @param name    command name
         * @param command command
         */
        public NameCommandPair(String name, SpecialCommand command) {
            this.name = name;
            this.command = command;
        }

        /**
         * get command name.
         *
         * @return command name
         */
        public String name() {
            return this.name;
        }

        /**
         * get command.
         *
         * @return command
         */
        public SpecialCommand command() {
            return this.command;
        }
    }

    /**
     * get matched command list.
     *
     * @param statement the target statement
     * @return command list
     */
    @Nonnull
    public static List<NameCommandPair> findCommand(@Nonnull SpecialStatement statement) {
        String commandName = statement.getCommandName().getValue();
        return findCommand(commandName);
    }

    /**
     * get matched command list.
     *
     * @param commandName command name
     * @return command list
     */
    @Nonnull
    public static List<NameCommandPair> findCommand(@Nonnull String commandName) {
        String inputName = toLowerCase(commandName);

        var result = new ArrayList<NameCommandPair>();
        for (var command : COMMAND_LIST) {
            var list = new ArrayList<>(command.getCommandNameList());
            list.sort((s1, s2) -> Integer.compare(s1.length(), s2.length()));
            for (var name : list) {
                if (name.equals(inputName)) {
                    return List.of(new NameCommandPair(name, command));
                }
                if (name.startsWith(inputName)) {
                    result.add(new NameCommandPair(name, command));
                    break;
                }
            }
        }

        return result;
    }

    private final boolean candidateEnd;
    private final List<String> commandNameList;

    /**
     * Creates a new instance.
     *
     * @param names command names
     */
    protected SpecialCommand(String... names) {
        this(true, names);
    }

    /**
     * Creates a new instance.
     *
     * @param candidateEnd whether to end completion
     * @param names        command names
     */
    protected SpecialCommand(boolean candidateEnd, String... names) {
        assert names.length >= 1;
        this.candidateEnd = candidateEnd;
        this.commandNameList = Collections.unmodifiableList(Arrays.stream(names).map(SpecialCommand::toLowerCase).collect(Collectors.toList()));
    }

    protected static String toLowerCase(String s) {
        return s.toLowerCase(Locale.ENGLISH);
    }

    /**
     * get representative command name.
     *
     * @return command name
     */
    public String getCommandName() {
        var index = 0;
        assert index < commandNameList.size();
        return commandNameList.get(index);
    }

    /**
     * get command names.
     *
     * @return command name list
     */
    @Nonnull
    public List<String> getCommandNameList() {
        return this.commandNameList;
    }

    /**
     * get completer candidates.
     *
     * @return candidates
     */
    public static List<CompleterCandidateWords> getCompleterCandidateList() {
        var result = new ArrayList<CompleterCandidateWords>();
        for (var command : COMMAND_LIST) {
            command.collectCompleterCandidate(result);
        }
        return result;
    }

    /**
     * collect completer candidates.
     *
     * @param result candidates
     */
    protected void collectCompleterCandidate(List<CompleterCandidateWords> result) {
        var list = getCommandNameList();
        for (String name : list) {
            result.add(new CompleterCandidateWords(COMMAND_PREFIX + name, this.candidateEnd));
        }
    }

    /**
     * get completer candidates.
     *
     * @param config     tgsql configuration
     * @param inputWords input words
     * @return candidates
     */
    public List<CompleterCandidateWords> getDynamicCompleterCandidateList(TgsqlConfig config, String[] inputWords) {
        return List.of();
    }

    /**
     * Executes a special statement.
     *
     * @param engine    BasicEngine
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    public abstract boolean execute(@Nonnull BasicEngine engine, @Nonnull SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException;

    protected static String getOption(SpecialStatement statement, int index) {
        var options = statement.getCommandOptions();
        if (index < options.size()) {
            return options.get(index).getValue();
        }
        return null;
    }

    /**
     * Execute as unknown option erroneous.
     *
     * @param engine    BasicEngine
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    protected boolean executeUnknownOption(@Nonnull BasicEngine engine, @Nonnull SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        return engine.execute(toUnknownError(statement, statement.getCommandOptions().get(0)));
    }

    /**
     * Returns an {@link ErroneousStatement} from the unknown command.
     *
     * @param statement the unknown command
     * @return corresponding {@link ErroneousStatement}
     */
    public static ErroneousStatement toUnknownError(SpecialStatement statement) {
        return new ErroneousStatement(//
                statement.getText(), //
                statement.getRegion(), //
                ErrorKind.UNKNOWN_SPECIAL_COMMAND, //
                statement.getCommandName().getRegion(), //
                MessageFormat.format(//
                        "unknown command: \"{0}\"", //
                        statement.getCommandName().getValue()));
    }

    /**
     * Returns an {@link ErroneousStatement} from the unknown command.
     *
     * @param statement the unknown command
     * @param nameList  ambiguous name list
     * @return corresponding {@link ErroneousStatement}
     */
    public static ErroneousStatement toUnknownError(SpecialStatement statement, List<String> nameList) {
        return new ErroneousStatement(//
                statement.getText(), //
                statement.getRegion(), //
                ErrorKind.UNKNOWN_SPECIAL_COMMAND, //
                statement.getCommandName().getRegion(), //
                MessageFormat.format(//
                        "ambiguous command: \"{0}\" in {1}", //
                        statement.getCommandName().getValue(), nameList));
    }

    /**
     * Returns an {@link ErroneousStatement} from the unknown command.
     *
     * @param statement the special command
     * @param option    the unknown option token in the command
     * @return corresponding {@link ErroneousStatement}
     */
    private static ErroneousStatement toUnknownError(SpecialStatement statement, Regioned<String> option) {
        return new ErroneousStatement(//
                statement.getText(), //
                statement.getRegion(), //
                ErrorKind.UNKNOWN_SPECIAL_COMMAND_OPTION, //
                option.getRegion(), //
                MessageFormat.format(//
                        "unrecognized option: \"{0}\"", //
                        option.getValue()));
    }

    protected void printHelp(BasicEngine engine) {
        String commandName = getCommandName();
        HelpCommand.printHelp(commandName, engine.getReporter());
    }
}
