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
package com.tsurugidb.tgsql.core.executor.engine;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.tgsql.core.model.CallStatement;
import com.tsurugidb.tgsql.core.model.CommitStatement;
import com.tsurugidb.tgsql.core.model.ErroneousStatement;
import com.tsurugidb.tgsql.core.model.ExplainStatement;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
import com.tsurugidb.tgsql.core.model.StartTransactionStatement;
import com.tsurugidb.tgsql.core.model.Statement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * An abstract implementation of {@link Engine}.
 */
public abstract class AbstractEngine implements Engine {

    @Override
    public boolean execute(@Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        switch (statement.getKind()) {
        case EMPTY:
            return executeEmptyStatement(statement);
        case START_TRANSACTION:
            return executeStartTransactionStatement((StartTransactionStatement) statement);
        case COMMIT:
            return executeCommitStatement((CommitStatement) statement);
        case ROLLBACK:
            return executeRollbackStatement(statement);
        case GENERIC:
            return executeGenericStatement(statement);
        case CALL:
            return executeCallStatement((CallStatement) statement);
        case EXPLAIN:
            return executeExplainStatement((ExplainStatement) statement);
        case SPECIAL:
            return executeSpecialStatement((SpecialStatement) statement);
        case ERRONEOUS:
            return executeErroneousStatement((ErroneousStatement) statement);
        }
        throw new AssertionError();
    }

    /**
     * Executes an erroneous statement.
     *
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    public abstract boolean executeErroneousStatement(@Nonnull ErroneousStatement statement) throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a special statement.
     *
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    public abstract boolean executeSpecialStatement(@Nonnull SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a call statement.
     *
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    public abstract boolean executeCallStatement(@Nonnull CallStatement statement) throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes an explain statement.
     *
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    public abstract boolean executeExplainStatement(@Nonnull ExplainStatement statement) throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a rollback statement.
     *
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    public abstract boolean executeRollbackStatement(@Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a commit statement.
     *
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    public abstract boolean executeCommitStatement(@Nonnull CommitStatement statement) throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a start transaction statement.
     *
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    public abstract boolean executeStartTransactionStatement(@Nonnull StartTransactionStatement statement) throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a generic statement.
     *
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    public abstract boolean executeGenericStatement(@Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes an empty statement.
     *
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException      if error occurred in engine itself
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    public abstract boolean executeEmptyStatement(@Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException;
}