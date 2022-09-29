package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @FunctionalInterface
    private interface Executor {
        boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException;
    }

    private static class SubCommand {
        private final String name;
        private final Executor executor;

        SubCommand(String name, Executor executor) {
            this.name = name;
            this.executor = executor;
        }

        String name() {
            return this.name;
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
        super("show"); //$NON-NLS-1$

        add("table", ShowCommand::executeShowTable); //$NON-NLS-1$
        add("transaction", ShowCommand::executeShowTransaction); //$NON-NLS-1$
    }

    private void add(String name, Executor executor) {
        subCommandList.add(new SubCommand(toLowerCase(name), executor));
    }

    @Override
    protected void collectCompleterCandidate(List<List<String>> result) {
        for (var command : subCommandList) {
            result.add(List.of("\\show", command.name())); //$NON-NLS-1$
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
            return engine.execute(toSubUnknownError(statement, "tableName"));
        }
        var sqlProcessor = engine.getSqlProcessor();
        var metadata = sqlProcessor.getTableMetadata(tableName);
        var reporter = engine.getReporter();
        reporter.reportTableMetadata(tableName, metadata);
        return true;
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
