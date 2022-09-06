package com.tsurugidb.console.core.executor;

import java.io.IOException;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.console.core.model.CallStatement;
import com.tsurugidb.console.core.model.CommitStatement;
import com.tsurugidb.console.core.model.ErroneousStatement;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.console.core.model.StartTransactionStatement;
import com.tsurugidb.console.core.model.Statement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * An abstract implementation of {@link Engine}.
 */
public abstract class AbstractEngine implements Engine {

    @Override
    public boolean execute(
            @Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException {
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
        case SPECIAL:
            return executeSpecialStatement((SpecialStatement) statement);
        case ERRONEOUS:
            return executeErroneousStatement((ErroneousStatement) statement);
        }
        throw new AssertionError();
    }

    /**
     * Executes an erroneous statement.
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException if error occurred in engine itself
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    protected abstract boolean executeErroneousStatement(@Nonnull ErroneousStatement statement)
            throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a special statement.
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException if error occurred in engine itself
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    protected abstract boolean executeSpecialStatement(@Nonnull SpecialStatement statement)
            throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a call statement.
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException if error occurred in engine itself
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    protected abstract boolean executeCallStatement(@Nonnull CallStatement statement)
            throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a rollback statement.
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException if error occurred in engine itself
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    protected abstract boolean executeRollbackStatement(@Nonnull Statement statement)
            throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a commit statement.
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException if error occurred in engine itself
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    protected abstract boolean executeCommitStatement(@Nonnull CommitStatement statement)
            throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a start transaction statement.
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException if error occurred in engine itself
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    protected abstract boolean executeStartTransactionStatement(@Nonnull StartTransactionStatement statement)
            throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes a generic statement.
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException if error occurred in engine itself
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    protected abstract boolean executeGenericStatement(@Nonnull Statement statement)
            throws EngineException, ServerException, IOException, InterruptedException;

    /**
     * Executes an empty statement.
     * @param statement the target statement
     * @return {@code true} to continue execution, {@code false} if shutdown was requested
     * @throws EngineException if error occurred in engine itself
     * @throws ServerException if server side error was occurred
     * @throws IOException if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    protected abstract boolean executeEmptyStatement(@Nonnull Statement statement)
            throws EngineException, ServerException, IOException, InterruptedException;
}