package com.tsurugidb.tgsql.cli.repl;

import java.io.IOException;

import com.tsurugidb.tgsql.core.config.ScriptConfig;
import com.tsurugidb.tgsql.core.executor.engine.AbstractEngine;
import com.tsurugidb.tgsql.core.executor.engine.Engine;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.executor.report.ScriptReporter;
import com.tsurugidb.tgsql.core.executor.sql.TransactionWrapper;
import com.tsurugidb.tgsql.core.model.CallStatement;
import com.tsurugidb.tgsql.core.model.CommitStatement;
import com.tsurugidb.tgsql.core.model.ErroneousStatement;
import com.tsurugidb.tgsql.core.model.ExplainStatement;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
import com.tsurugidb.tgsql.core.model.StartTransactionStatement;
import com.tsurugidb.tgsql.core.model.Statement;
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
    public ScriptConfig getConfig() {
        return delegate.getConfig();
    }

    @Override
    public ScriptReporter getReporter() {
        return delegate.getReporter();
    }

    @Override
    public void connect() throws ServerException, IOException, InterruptedException {
        delegate.connect();
    }

    @Override
    public TransactionWrapper getTransaction() {
        return delegate.getTransaction();
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

    @Override
    public void finish(boolean succeed) throws IOException {
        delegate.finish(succeed);
    }
}
