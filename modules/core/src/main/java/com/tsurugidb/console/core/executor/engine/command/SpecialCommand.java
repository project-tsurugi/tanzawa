package com.tsurugidb.console.core.executor.engine.command;

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

import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.model.ErroneousStatement;
import com.tsurugidb.console.core.model.ErroneousStatement.ErrorKind;
import com.tsurugidb.console.core.model.Regioned;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Special command for Tsurugi SQL console.
 */
public abstract class SpecialCommand {

    /**
     * special command prefix
     */
    public static final String COMMAND_PREFIX = "\\"; //$NON-NLS-1$

    private static final SpecialCommand[] COMMAND_LIST = { //
            new ExitCommand(), //
            new HaltCommand(), //
            new HelpCommand(), //
            new ShowCommand(), //
            new StatusCommand(), //
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

    private final List<String> commandNameList;

    /**
     * Creates a new instance.
     * 
     * @param names command names
     */
    protected SpecialCommand(String... names) {
        assert names.length >= 1;
        this.commandNameList = Collections.unmodifiableList(Arrays.stream(names).map(SpecialCommand::toLowerCase).collect(Collectors.toList()));
    }

    protected static String toLowerCase(String s) {
        return s.toLowerCase(Locale.ENGLISH);
    }

    /**
     * get representative command name
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
    public static List<List<String>> getCompleterCandidateList() {
        var result = new ArrayList<List<String>>();
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
    protected void collectCompleterCandidate(List<List<String>> result) {
        var list = getCommandNameList();
        for (String name : list) {
            result.add(List.of(COMMAND_PREFIX + name));
        }
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
