package com.tsurugidb.console.core.executor.engine;

import java.io.IOException;

import javax.annotation.Nonnull;

import com.tsurugidb.console.core.executor.report.ScriptReporter;
import com.tsurugidb.console.core.model.Statement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * An interface of Tsurugi SQL console engine.
 */
@FunctionalInterface
public interface Engine {

    /**
     * get reporter.
     * 
     * @return script reporter
     */
    default ScriptReporter getReporter() {
        throw new UnsupportedOperationException("do override");
    }

    /**
     * Executes a statement.
     * 
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    boolean execute(@Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * finish Engine
     * 
     * @param succeed {@code true} to successful end
     * @throws IOException if I/O error was occurred while executing the statement
     */
    default void finish(boolean succeed) throws IOException {
        // do override
    }
}
