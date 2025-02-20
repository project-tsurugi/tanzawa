/*
 * Copyright 2023-2025 Project Tsurugi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tsurugidb.tgsql.core.executor.engine.command;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.config.TgsqlCvKey;
import com.tsurugidb.tgsql.core.executor.engine.BasicEngine;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.executor.report.TgsqlReporter;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
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
        var keys = TgsqlCvKey.getKeyNames();
        for (String name : keys) {
            var candidate = new CompleterCandidateWords(name.endsWith("."));
            candidate.add(prefixList);
            candidate.add(name);

            result.add(candidate);
        }
    }

    @Override
    public List<CompleterCandidateWords> getDynamicCompleterCandidateList(TgsqlConfig config, String[] inputWords) {
        if (inputWords.length != 2) {
            return List.of();
        }

        return getDynamicCompleterCandidateList(config, List.of(COMMAND));
    }

    protected static List<CompleterCandidateWords> getDynamicCompleterCandidateList(TgsqlConfig config, List<String> prefixList) {
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

    protected static void showClientVariable(String key, Object value, TgsqlReporter reporter) {
        String v = String.valueOf(value);
        var message = MessageFormat.format("{0}={1}", key, v); //$NON-NLS-1$
        reporter.info(message);
    }
}
