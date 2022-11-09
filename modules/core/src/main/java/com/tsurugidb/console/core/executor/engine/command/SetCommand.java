package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.executor.report.ScriptReporter;
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

        if (sb != null) {
            LOG.debug("set client variable. key={}, value=[{}]", key, sb); //$NON-NLS-1$
            return executeSet(engine, key, sb.toString());
        } else {
            LOG.debug("show client variable. key={}", key); //$NON-NLS-1$
            return executeShow(engine, key);
        }
    }

    protected static boolean executeSet(BasicEngine engine, String key, String value) {
        var clientVariableMap = engine.getConfig().getClientVariableMap();
        clientVariableMap.put(key, value);

        showClientVariable(key, value, engine.getReporter());
        return true;
    }

    protected static boolean executeShow(BasicEngine engine, String key) {
        String keyPrefix = (key != null) ? key : "";

        var map = engine.getConfig().getClientVariableMap();
        var reporter = engine.getReporter();
        for (var entry : map.entrySet()) {
            String k = entry.getKey();
            if (k.startsWith(keyPrefix)) {
                String v = String.valueOf(entry.getValue());
                showClientVariable(k, v, reporter);
            }
        }
        return true;
    }

    protected static void showClientVariable(String key, String value, ScriptReporter reporter) {
        var message = MessageFormat.format("{0}={1}", key, value); //$NON-NLS-1$
        reporter.info(message);
    }
}
