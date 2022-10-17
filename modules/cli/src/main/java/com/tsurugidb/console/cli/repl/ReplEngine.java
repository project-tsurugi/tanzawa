package com.tsurugidb.console.cli.repl;

import java.io.IOException;

import com.tsurugidb.console.core.executor.engine.AbstractEngine;
import com.tsurugidb.console.core.executor.engine.Engine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.executor.report.ScriptReporter;
import com.tsurugidb.console.core.model.CallStatement;
import com.tsurugidb.console.core.model.CommitStatement;
import com.tsurugidb.console.core.model.ErroneousStatement;
import com.tsurugidb.console.core.model.ExplainStatement;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.console.core.model.StartTransactionStatement;
import com.tsurugidb.console.core.model.Statement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Tsurugi SQL console repl {@link Engine}.
 */
public class ReplEngine extends AbstractEngine {

    private final AbstractEngine delegate;
    private final ReplThreadExecutor threadExecutor;

    /**
     * Creates a new instance.
     *
     * @param delegate AbstractEngine
     * @param executor thread executor
     */
    public ReplEngine(AbstractEngine delegate, ReplThreadExecutor executor) {
        this.delegate = delegate;
        this.threadExecutor = executor;
    }

    @Override
    public ScriptReporter getReporter() {
        return delegate.getReporter();
    }

    @Override
    public boolean executeErroneousStatement(ErroneousStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        return delegate.executeErroneousStatement(statement);
    }

    @Override
    public boolean executeSpecialStatement(SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        return delegate.executeSpecialStatement(statement);
    }

    @Override
    public boolean executeCallStatement(CallStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        return threadExecutor.invoke(() -> delegate.executeCallStatement(statement));
    }

    @Override
    public boolean executeExplainStatement(ExplainStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        return threadExecutor.invoke(() -> delegate.executeExplainStatement(statement));
    }

    @Override
    public boolean executeRollbackStatement(Statement statement) throws EngineException, ServerException, IOException, InterruptedException {
        return threadExecutor.invoke(() -> delegate.executeRollbackStatement(statement));
    }

    @Override
    public boolean executeCommitStatement(CommitStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        return threadExecutor.invoke(() -> delegate.executeCommitStatement(statement));
    }

    @Override
    public boolean executeStartTransactionStatement(StartTransactionStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        return threadExecutor.invoke(() -> delegate.executeStartTransactionStatement(statement));
    }

    @Override
    public boolean executeGenericStatement(Statement statement) throws EngineException, ServerException, IOException, InterruptedException {
        return threadExecutor.invoke(() -> delegate.executeGenericStatement(statement));
    }

    @Override
    public boolean executeEmptyStatement(Statement statement) throws EngineException, ServerException, IOException, InterruptedException {
        return delegate.executeEmptyStatement(statement);
    }
}
