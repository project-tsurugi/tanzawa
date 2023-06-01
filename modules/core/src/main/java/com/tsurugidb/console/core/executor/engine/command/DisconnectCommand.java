package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Disconnect command for Tsurugi SQL console.
 */
public class DisconnectCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(DisconnectCommand.class);

    /**
     * Creates a new instance.
     */
    public DisconnectCommand() {
        super("disconnect"); //$NON-NLS-1$
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        LOG.debug("starting disconnect"); //$NON-NLS-1$

        boolean result = engine.disconnect();

        var reporter = engine.getReporter();
        reporter.reportDisconnect(result);

        return true;
    }
}
