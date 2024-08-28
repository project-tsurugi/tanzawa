/*
 * Copyright 2023-2024 Project Tsurugi.
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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.config.TgsqlCvKey;
import com.tsurugidb.tgsql.core.executor.engine.BasicEngine;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Timing command for Tsurugi SQL console.
 */
public class TimingCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(TimingCommand.class);

    private static final String COMMAND_NAME = "timing"; //$NON-NLS-1$
    private static final String COMMAND = COMMAND_PREFIX + COMMAND_NAME;

    /**
     * Creates a new instance.
     */
    public TimingCommand() {
        super(COMMAND_NAME);
    }

    @Override
    protected void collectCompleterCandidate(List<CompleterCandidateWords> result) {
        result.add(new CompleterCandidateWords(COMMAND, true));
        result.add(new CompleterCandidateWords(COMMAND, "on", true));
        result.add(new CompleterCandidateWords(COMMAND, "off", true));
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        var clientVariableMap = engine.getConfig().getClientVariableMap();
        var option = getOption(statement, 0);

        boolean value;
        if (option == null) {
            LOG.debug("timing"); //$NON-NLS-1$
            value = !clientVariableMap.get(TgsqlCvKey.SQL_TIMING, false);
        } else {
            LOG.debug("timing {}", option); //$NON-NLS-1$
            value = TgsqlCvKey.SQL_TIMING.convertValue(option);
        }

        clientVariableMap.put(TgsqlCvKey.SQL_TIMING, value);

        var message = MessageFormat.format("Timing is {0}.", value ? "on" : "off"); //$NON-NLS-1$
        var reporter = engine.getReporter();
        reporter.info(message);

        return true;
    }
}
