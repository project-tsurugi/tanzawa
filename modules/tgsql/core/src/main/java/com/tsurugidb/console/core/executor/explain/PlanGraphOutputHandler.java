package com.tsurugidb.console.core.executor.explain;

import javax.annotation.Nonnull;

import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.executor.report.ScriptReporter;
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
    void handle(@Nonnull ScriptReporter reporter, @Nonnull PlanGraph graph)
            throws EngineException, InterruptedException;
}
