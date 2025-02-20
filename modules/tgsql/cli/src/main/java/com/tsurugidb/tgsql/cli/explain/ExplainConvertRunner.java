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
package com.tsurugidb.tgsql.cli.explain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import com.tsurugidb.tgsql.cli.argument.CliArgument;
import com.tsurugidb.tgsql.cli.config.ExplainConfigBuilder;
import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.executor.engine.CommandPath;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.executor.explain.DotOutputHandler;
import com.tsurugidb.tgsql.core.executor.report.BasicReporter;
import com.tsurugidb.tgsql.core.executor.report.PlanGraphReporter;
import com.tsurugidb.tgsql.core.model.Regioned;
import com.tsurugidb.tgsql.core.model.Value;
import com.tsurugidb.tsubakuro.explain.PlanGraph;
import com.tsurugidb.tsubakuro.explain.PlanGraphException;
import com.tsurugidb.tsubakuro.explain.json.JsonPlanGraphLoader;

/**
 * Convert explain json file.
 */
public final class ExplainConvertRunner {

    /**
     * Convert explain.
     *
     * @param argument explain argument
     * @throws Exception if exception was occurred
     */
    public static void execute(CliArgument argument) throws Exception {
        argument.checkUnknownParameter();

        var builder = new ExplainConfigBuilder(argument);
        var config = builder.build();
        new ExplainConvertRunner(argument, config).execute();
    }

    private final CliArgument argument;
    private final TgsqlConfig config;

    private ExplainConvertRunner(CliArgument argument, TgsqlConfig config) {
        this.argument = argument;
        this.config = config;
    }

    /**
     * Convert explain.
     *
     * @throws Exception if exception was occurred
     */
    public void execute() throws Exception {
        var contents = readContents();
        var plan = getPlanGraph(contents);

        if (argument.isReport()) {
            executeReport(plan);
        }
        if (argument.getOutputFile() != null) {
            executeDot(plan);
        }
    }

    private String readContents() throws IOException {
        var path = Path.of(argument.getInputFile());
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private PlanGraph getPlanGraph(String contents) throws PlanGraphException {
        var builder = JsonPlanGraphLoader.newBuilder();
        if (argument.isVerbose()) {
            builder.withNodeFilter(node -> true);
        }
        var loader = builder.build();

        return loader.load(contents);
    }

    private void executeReport(PlanGraph plan) {
        var reporter = new PlanGraphReporter(System.out::println);
        reporter.report("unknown SQL", plan);
    }

    private void executeDot(PlanGraph plan) throws EngineException, InterruptedException {
        var options = getOptions();
        var path = CommandPath.system();
        var handler = DotOutputHandler.fromOptions(options, path);

        handler.handle(new BasicReporter(config), plan);
    }

    private Map<Regioned<String>, Optional<Regioned<Value>>> getOptions() {
        var clientVariableMap = config.getClientVariableMap();
        clientVariableMap.put(DotOutputHandler.KEY_OUTPUT, argument.getOutputFile());
        if (argument.isVerbose()) {
            clientVariableMap.put(DotOutputHandler.KEY_VERBOSE, Boolean.TRUE.toString());
        }

        return DotOutputHandler.extendOptions(Map.of(), clientVariableMap);
    }
}
