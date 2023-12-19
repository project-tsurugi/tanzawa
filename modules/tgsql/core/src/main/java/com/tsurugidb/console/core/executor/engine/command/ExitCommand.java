package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Exit command for Tsurugi SQL console.
 */
public class ExitCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ExitCommand.class);

    /**
     * Creates a new instance.
     */
    public ExitCommand() {
        super("exit", "quit"); //$NON-NLS-1$
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        if (!statement.getCommandOptions().isEmpty()) {
            return executeUnknownOption(engine, statement);
        }
        LOG.debug("starting shut-down"); //$NON-NLS-1$
        engine.checkTransactionInactive(statement);
        return false;
    }
}
