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
package com.tsurugidb.tgsql.cli.repl;

import java.io.IOException;

import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.executor.engine.AbstractEngine;
import com.tsurugidb.tgsql.core.executor.engine.Engine;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.executor.report.TgsqlReporter;
import com.tsurugidb.tgsql.core.executor.sql.TransactionWrapper;
import com.tsurugidb.tgsql.core.model.CallStatement;
import com.tsurugidb.tgsql.core.model.CommitStatement;
import com.tsurugidb.tgsql.core.model.ErroneousStatement;
import com.tsurugidb.tgsql.core.model.ExplainStatement;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
import com.tsurugidb.tgsql.core.model.StartTransactionStatement;
import com.tsurugidb.tgsql.core.model.Statement;
import com.tsurugidb.tsubakuro.common.Session;
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
    public TgsqlConfig getConfig() {
        return delegate.getConfig();
    }

    @Override
    public TgsqlReporter getReporter() {
        return delegate.getReporter();
    }

    @Override
    public void connect() throws ServerException, IOException, InterruptedException {
        delegate.connect();
    }

    @Override
    public Session getSession() {
        return delegate.getSession();
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
