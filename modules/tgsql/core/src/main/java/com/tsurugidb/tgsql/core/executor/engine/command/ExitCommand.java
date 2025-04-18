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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.executor.engine.BasicEngine;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
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
