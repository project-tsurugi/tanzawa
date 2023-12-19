package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Status command for Tsurugi SQL console.
 */
public class StatusCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(StatusCommand.class);

    /**
     * Creates a new instance.
     */
    public StatusCommand() {
        super("status"); //$NON-NLS-1$
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        if (!statement.getCommandOptions().isEmpty()) {
            // TODO more status?
            return executeUnknownOption(engine, statement);
        }
        LOG.debug("show status"); //$NON-NLS-1$
        return ShowCommand.executeShowTransaction(engine);
    }
}
