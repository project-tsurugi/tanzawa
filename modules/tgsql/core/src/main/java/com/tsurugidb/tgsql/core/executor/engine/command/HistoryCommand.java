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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.exception.TgsqlMessageException;
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
            throw new TgsqlMessageException(MessageFormat.format("not integer. option={0}", option), e);
        }
    }
}
