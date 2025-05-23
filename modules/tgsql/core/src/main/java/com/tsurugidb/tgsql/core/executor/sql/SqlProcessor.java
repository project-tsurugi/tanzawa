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
package com.tsurugidb.tgsql.core.executor.sql;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.model.Region;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.SqlServiceException;
import com.tsurugidb.tsubakuro.sql.StatementMetadata;
import com.tsurugidb.tsubakuro.sql.TableMetadata;
import com.tsurugidb.tsubakuro.sql.TransactionStatus.TransactionStatusWithMessage;
import com.tsurugidb.tsubakuro.util.ServerResource;

/**
 * Processes SQL-related actions.
 */
public interface SqlProcessor extends ServerResource {

    /**
     * connect.
     *
     * @param config config
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    void connect(TgsqlConfig config) throws ServerException, IOException, InterruptedException;

    /**
     * get endpoint.
     *
     * @return endpoint
     */
    default @Nullable String getEndpoint() {
        return null;
    }

    /**
     * get SqlClient.
     *
     * @return SqlClient
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    default SqlClient getSqlClient() throws ServerException, IOException, InterruptedException {
        throw new AssertionError("do override");
    }

    /**
     * disconnect.
     *
     * @return {@code false} if already disconnected
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    boolean disconnect() throws ServerException, IOException, InterruptedException;

    /**
     * Returns a list of the available table names in the database, except system tables.
     *
     * @return a list of the available table names
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    List<String> getTableNames() throws ServerException, IOException, InterruptedException;

    /**
     * Returns table metadata.
     *
     * @param tableName table name
     * @return table metadata, or null if table not found
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    @Nullable
    TableMetadata getTableMetadata(String tableName) throws ServerException, IOException, InterruptedException;

    /**
     * Returns whether or not the holding session is active.
     *
     * @return {@code true} if the holding session is (probably) active, or {@code false} otherwise
     */
    boolean isSessionActive();

    /**
     * Returns whether or not the holding transaction is active.
     *
     * @return {@code true} if the holding transaction is (probably) active, or {@code false} otherwise
     */
    boolean isTransactionActive();

    /**
     * Returns the running transaction.
     *
     * @return the running transaction, or {@code null} if there is no active transactions
     */
    @Nullable
    TransactionWrapper getTransaction();

    /**
     * Returns the running transaction.
     *
     * @return the running transaction
     * @throws IllegalStateException if there is no active transactions
     */
    default TransactionWrapper getTransactionOrThrow() {
        var transaction = getTransaction();
        if (transaction == null) {
            throw new IllegalStateException("transaction is not running");
        }
        return transaction;
    }

    /**
     * Provides transaction id that is unique to for the duration of the database server's lifetime.
     *
     * @return the id String for this transaction
     */
    String getTransactionId();

    /**
     * Returns occurred error in the target transaction, only if the transaction has been accidentally aborted.
     *
     * @return the error information
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while retrieving transaction error
     * @throws InterruptedException if interrupted while retrieving transaction error
     */
    SqlServiceException getTransactionException() throws ServerException, IOException, InterruptedException;

    /**
     * Returns status in the target transaction.
     *
     * @return status
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while retrieving transaction error
     * @throws InterruptedException if interrupted while retrieving transaction error
     */
    TransactionStatusWithMessage getTransactionStatus() throws ServerException, IOException, InterruptedException;

    /**
     * Executes a SQL statement.
     *
     * @param statement the target SQL statement text
     * @param region    the region of the statement in the document
     * @return the result set of the execution, or {@code null} if the statement does not returns any results
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    @Nullable
    PreparedStatementResult execute(@Nonnull String statement, @Nullable Region region) throws ServerException, IOException, InterruptedException;

    /**
     * Starts a new transaction. After this operation, this object will hold the started transaction as active.
     *
     * @param option the transaction option
     * @throws IllegalStateException if another transaction is active in this object
     * @throws ServerException       if server side error was occurred
     * @throws IOException           if I/O error was occurred while starting a transaction
     * @throws InterruptedException  if interrupted while starting a transaction
     */
    void startTransaction(@Nonnull SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException;

    /**
     * Commits the current transaction. After this operation, this object will releases the holding transaction.
     *
     * @param status the commit status wait for
     * @throws IllegalStateException if any transactions are active in this object
     * @throws ServerException       if server side error was occurred
     * @throws IOException           if I/O error was occurred while committing the transaction
     * @throws InterruptedException  if interrupted while committing the transaction
     */
    void commitTransaction(@Nonnull SqlRequest.CommitStatus status) throws ServerException, IOException, InterruptedException;

    /**
     * Aborts the current transaction. After this operation, this object will releases the holding transaction.
     *
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while committing the transaction
     * @throws InterruptedException if interrupted while committing the transaction
     */
    void rollbackTransaction() throws ServerException, IOException, InterruptedException;

    /**
     * Inspects a SQL statement.
     *
     * @param statement the target SQL statement text
     * @param region    the region of the statement in the document
     * @return the inspected metadata of the statement
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while inspecting the statement
     * @throws InterruptedException if interrupted while inspecting the statement
     */
    StatementMetadata explain(@Nonnull String statement, @Nullable Region region) throws ServerException, IOException, InterruptedException;

    @Override
    default void close() throws ServerException, IOException, InterruptedException {
        return;
    }
}
