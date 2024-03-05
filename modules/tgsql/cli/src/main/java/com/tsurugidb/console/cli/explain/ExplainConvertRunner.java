package com.tsurugidb.console.cli.explain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import com.tsurugidb.console.cli.argument.CliArgument;
import com.tsurugidb.console.cli.config.ExplainConfigBuilder;
import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.executor.engine.CommandPath;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.executor.explain.DotOutputHandler;
import com.tsurugidb.console.core.executor.report.BasicReporter;
import com.tsurugidb.console.core.executor.report.PlanGraphReporter;
import com.tsurugidb.console.core.model.Regioned;
import com.tsurugidb.console.core.model.Value;
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
    private final ScriptConfig config;

    private ExplainConvertRunner(CliArgument argument, ScriptConfig config) {
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

        handler.handle(new BasicReporter(), plan);
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
