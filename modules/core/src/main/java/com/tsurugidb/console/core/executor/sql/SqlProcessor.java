package com.tsurugidb.console.core.executor.sql;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.StatementMetadata;
import com.tsurugidb.tsubakuro.sql.TableMetadata;
import com.tsurugidb.tsubakuro.util.ServerResource;
import com.tsurugidb.console.core.model.Region;
import com.tsurugidb.sql.proto.SqlRequest;

/**
 * Processes SQL-related actions.
 */
public interface SqlProcessor extends ServerResource {

    /**
     * Returns table metadata.
     * @param tableName table name
     * @return table metadata, or null if table not found
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    @Nullable
    TableMetadata getTableMetadata(String tableName) throws ServerException, IOException, InterruptedException;

    /**
     * Returns whether or not the holding transaction is active.
     * @return {@code true} if the holding transaction is (probably) active, or {@code false} otherwise
     */
    boolean isTransactionActive();

    /**
     * Executes a SQL statement.
     * @param statement the target SQL statement text
     * @param region the region of the statement in the document
     * @return the result set of the execution, or {@code null} if the statement does not returns any results
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    @Nullable ResultSet execute(
            @Nonnull String statement,
            @Nullable Region region) throws ServerException, IOException, InterruptedException;

    /**
     * Starts a new transaction.
     * After this operation, this object will hold the started transaction as active.
     * @param option the transaction option
     * @throws IllegalStateException if another transaction is active in this object
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while starting a transaction
     * @throws InterruptedException if interrupted while starting a transaction
     */
    void startTransaction(
            @Nonnull SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException;

    /**
     * Commits the current transaction.
     * After this operation, this object will releases the holding transaction.
     * @param status the commit status wait for
     * @throws IllegalStateException if any transactions are active in this object
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while committing the transaction
     * @throws InterruptedException if interrupted while committing the transaction
     */
    void commitTransaction(
            @Nonnull SqlRequest.CommitStatus status) throws ServerException, IOException, InterruptedException;

    /**
     * Aborts the current transaction.
     * After this operation, this object will releases the holding transaction.
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while committing the transaction
     * @throws InterruptedException if interrupted while committing the transaction
     */
    void rollbackTransaction() throws ServerException, IOException, InterruptedException;

    /**
     * Inspects a SQL statement.
     * @param statement the target SQL statement text
     * @param region the region of the statement in the document
     * @return the inspected metadata of the statement
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while inspecting the statement
     * @throws InterruptedException if interrupted while inspecting the statement
     */
    StatementMetadata explain(
            @Nonnull String statement,
            @Nullable Region region) throws ServerException, IOException, InterruptedException;

    @Override
    default void close() throws ServerException, IOException, InterruptedException {
        return;
    }
}
