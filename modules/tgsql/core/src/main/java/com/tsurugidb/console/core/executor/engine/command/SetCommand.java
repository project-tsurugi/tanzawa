package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.config.ScriptCvKey;
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

    private static final String COMMAND_NAME = "set"; //$NON-NLS-1$
    private static final String COMMAND = COMMAND_PREFIX + COMMAND_NAME;

    /**
     * Creates a new instance.
     */
    public SetCommand() {
        super(COMMAND_NAME);
    }

    @Override
    protected void collectCompleterCandidate(List<CompleterCandidateWords> result) {
        collectCompleterCandidate(result, List.of(COMMAND));
    }

    protected static void collectCompleterCandidate(List<CompleterCandidateWords> result, List<String> prefixList) {
        var keys = ScriptCvKey.getKeyNames();
        for (String name : keys) {
            var candidate = new CompleterCandidateWords(name.endsWith("."));
            candidate.add(prefixList);
            candidate.add(name);

            result.add(candidate);
        }
    }

    @Override
    public List<CompleterCandidateWords> getDynamicCompleterCandidateList(ScriptConfig config, String[] inputWords) {
        if (inputWords.length != 2) {
            return List.of();
        }

        return getDynamicCompleterCandidateList(config, List.of(COMMAND));
    }

    protected static List<CompleterCandidateWords> getDynamicCompleterCandidateList(ScriptConfig config, List<String> prefixList) {
        var clientVariableMap = config.getClientVariableMap();
        var result = new ArrayList<CompleterCandidateWords>(clientVariableMap.size());
        for (var entry : clientVariableMap.entrySet()) {
            String name = entry.getKey();
            var candidate = new CompleterCandidateWords(name.endsWith("."));
            candidate.add(prefixList);
            candidate.add(name);

            result.add(candidate);
        }
        return result;
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
        var convertedValue = clientVariableMap.put(key, value);

        showClientVariable(key, convertedValue, engine.getReporter());
        return true;
    }

    protected static boolean executeShow(BasicEngine engine, String key) {
        String keyPrefix = (key != null) ? key : "";

        var map = engine.getConfig().getClientVariableMap();
        var reporter = engine.getReporter();
        for (var entry : map.entrySet()) {
            String k = entry.getKey();
            if (k.startsWith(keyPrefix)) {
                showClientVariable(k, entry.getValue(), reporter);
            }
        }
        return true;
    }

    protected static void showClientVariable(String key, Object value, ScriptReporter reporter) {
        String v = String.valueOf(value);
        var message = MessageFormat.format("{0}={1}", key, v); //$NON-NLS-1$
        reporter.info(message);
    }
}
