package com.tsurugidb.tgsql.core.executor.engine.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.executor.engine.BasicEngine;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Halt command for Tsurugi SQL console.
 */
public class HaltCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HaltCommand.class);

    /**
     * Creates a new instance.
     */
    public HaltCommand() {
        super("halt"); //$NON-NLS-1$
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        if (!statement.getCommandOptions().isEmpty()) {
            return executeUnknownOption(engine, statement);
        }
        LOG.debug("starting force shut-down"); //$NON-NLS-1$
        return false;
    }
}
