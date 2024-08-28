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
package com.tsurugidb.tgsql.core.executor.explain;

import javax.annotation.Nonnull;

import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.executor.report.TgsqlReporter;
import com.tsurugidb.tsubakuro.explain.PlanGraph;

/**
 * Handles {@link PlanGraph} and output them.
 */
@FunctionalInterface
public interface PlanGraphOutputHandler extends OptionHandler {

    /**
     * Handles {@link PlanGraph} and save or displays it.
     * @param reporter the reporter
     * @param graph the input plan graph
     * @throws EngineException if error was occurred while processing the plan graph
     * @throws InterruptedException if interrupted while processing the plan graph
     */
    void handle(@Nonnull TgsqlReporter reporter, @Nonnull PlanGraph graph)
            throws EngineException, InterruptedException;
}
