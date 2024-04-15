package com.tsurugidb.tgsql.core.executor.engine.command;

import java.io.IOException;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.exception.ScriptMessageException;
import com.tsurugidb.tgsql.core.executor.engine.BasicEngine;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * History command for Tsurugi SQL console.
 */
public class HistoryCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HistoryCommand.class);

    /**
     * Creates a new instance.
     */
    public HistoryCommand() {
        super("history"); //$NON-NLS-1$
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        LOG.debug("starting show history"); //$NON-NLS-1$
        int size = getSize(statement);
        var config = engine.getConfig();
        var history = config.getHistory(size);
        var reporter = engine.getReporter();
        reporter.reportHistory(history);
        return true;
    }

    private int getSize(SpecialStatement statement) {
        String option = getOption(statement, 0);
        if (option == null) {
            return -1;
        }
        try {
            return Integer.parseInt(option);
        } catch (NumberFormatException e) {
            throw new ScriptMessageException(MessageFormat.format("not integer. option={0}", option), e);
        }
    }
}
