package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.model.ErroneousStatement;
import com.tsurugidb.console.core.model.ErroneousStatement.ErrorKind;
import com.tsurugidb.console.core.model.Regioned;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Status command for Tsurugi SQL console.
 */
public class ShowCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ShowCommand.class);

    private static final String COMMAND_NAME = "show"; //$NON-NLS-1$
    private static final String COMMAND = COMMAND_PREFIX + COMMAND_NAME;
    private static final String CLIENT = "client"; //$NON-NLS-1$

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
    public ShowCommand() {
        super(COMMAND_NAME);

        add("table", true, ShowCommand::executeShowTable); //$NON-NLS-1$
        add("transaction", false, ShowCommand::executeShowTransaction); //$NON-NLS-1$
        add(CLIENT, true, ShowCommand::executeShowClient); // $NON-NLS-1$
    }

    private void add(String name, boolean hasParameter, Executor executor) {
        subCommandList.add(new SubCommand(toLowerCase(name), hasParameter, executor));
    }

    @Override
    protected void collectCompleterCandidate(List<CompleterCandidateWords> result) {
        for (var command : subCommandList) {
            if (command.name().equals(CLIENT)) {
                SetCommand.collectCompleterCandidate(result, List.of(COMMAND, CLIENT));
            } else {
                result.add(new CompleterCandidateWords(COMMAND, command.name(), !command.hasParameter()));
            }
        }
    }

    @Override
    public List<CompleterCandidateWords> getDynamicCompleterCandidateList(ScriptConfig config, String[] inputWords) {
        if (inputWords.length != 3) {
            return List.of();
        }
        if (!inputWords[1].equals(CLIENT)) {
            return List.of();
        }

        return SetCommand.getDynamicCompleterCandidateList(config, List.of(COMMAND, CLIENT));
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

    private static boolean executeShowTransaction(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        LOG.debug("show transaction status"); //$NON-NLS-1$
        var sqlProcessor = engine.getSqlProcessor();
        boolean active = sqlProcessor.isTransactionActive();
        var reporter = engine.getReporter();
        reporter.reportTransactionStatus(active);
        return true;
    }

    private static boolean executeShowTable(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        String tableName = getOption(statement, 1);
        LOG.debug("show table. tableName={}", tableName); //$NON-NLS-1$
        if (tableName == null) {
            return engine.execute(toSubUnknownError(statement, "tableName")); //$NON-NLS-1$
        }
        var sqlProcessor = engine.getSqlProcessor();
        var metadata = sqlProcessor.getTableMetadata(tableName);
        var reporter = engine.getReporter();
        reporter.reportTableMetadata(tableName, metadata);
        return true;
    }

    private static boolean executeShowClient(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        String key = getOption(statement, 1);
        LOG.debug("show client. key={}", key); //$NON-NLS-1$
        return SetCommand.executeShow(engine, key);
    }

    private static String getOption(SpecialStatement statement, int index) {
        var options = statement.getCommandOptions();
        if (index < options.size()) {
            return options.get(index).getValue();
        }
        return null;
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

    private static ErroneousStatement toSubUnknownError(SpecialStatement statement, String name) {
        Regioned<String> option = statement.getCommandOptions().get(0);
        return new ErroneousStatement(//
                statement.getText(), //
                statement.getRegion(), //
                ErrorKind.UNKNOWN_SPECIAL_COMMAND_OPTION, //
                option.getRegion(), //
                MessageFormat.format(//
                        "unrecognized {0} option", //
                        name));
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
