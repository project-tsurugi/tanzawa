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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.executor.report.TgsqlReporter;
import com.tsurugidb.tgsql.core.executor.sql.TransactionWrapper;
import com.tsurugidb.tgsql.core.model.Statement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * An interface of Tsurugi SQL console engine.
 */
@FunctionalInterface
public interface Engine {

    /**
     * get tgsql configuration.
     *
     * @return tgsql configuration
     */
    default TgsqlConfig getConfig() {
        throw new UnsupportedOperationException("do override");
    }

    /**
     * get reporter.
     *
     * @return tgsql reporter
     */
    default TgsqlReporter getReporter() {
        throw new UnsupportedOperationException("do override");
    }

    /**
     * connect.
     *
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    default void connect() throws ServerException, IOException, InterruptedException {
        // do override
    }

    /**
     * disconnect.
     *
     * @return {@code false} if already disconnected
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    default boolean disconnect() throws ServerException, IOException, InterruptedException {
        return false; // do override
    }

    /**
     * Returns the running transaction.
     *
     * @return the running transaction, or {@code null} if there is no active transactions
     */
    default @Nullable TransactionWrapper getTransaction() {
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
     * finish Engine.
     *
     * @param succeed {@code true} to successful end
     * @throws IOException if I/O error was occurred while executing the statement
     */
    default void finish(boolean succeed) throws IOException {
        // do override
    }
}
