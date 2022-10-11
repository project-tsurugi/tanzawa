package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Set command for Tsurugi SQL console.
 */
public class SetCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(SetCommand.class);

    /**
     * Creates a new instance.
     */
    public SetCommand() {
        super("set"); //$NON-NLS-1$
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        String key = null;
        StringBuilder sb = null;
        for (var arg : statement.getCommandOptions()) {
            String value = arg.getValue();
            if (value == null) {
                continue;
            }

            if (key == null) {
                key = value;
                continue;
            }
            if (sb == null) {
                sb = new StringBuilder();
            } else {
                sb.append(" ");
            }
            sb.append(value);
        }
        if (key == null) {
            printHelp(engine);
            return true;
        }
        String value = (sb != null) ? sb.toString() : null;

        LOG.debug("set client variable"); //$NON-NLS-1$

        var config = engine.getConfig();
        config.setClientVariable(key, value);

        ShowCommand.showClientVariable(key, value, engine.getReporter());
        return true;
    }
}
