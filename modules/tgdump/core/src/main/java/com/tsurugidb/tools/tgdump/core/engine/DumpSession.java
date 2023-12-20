package com.tsurugidb.tools.tgdump.core.engine;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;

/**
 * Represents a database session to operate dump operation.
 */
public interface DumpSession extends AutoCloseable {

    /**
     * Registers dump target table, and returns its table metadata.
     * <p>
     * This cannot use after calling {@link #begin(DumpMonitor)}.
     * </p>
     * @param monitor the operation monitor
     * @param target the dump target table information
     * @throws InterruptedException if interrupted during the operation
     * @throws DiagnosticException if the target table is not found in the database
     * @throws DiagnosticException if error was occurred
     * @throws IllegalStateException if the transaction have been already started
     * @see #begin(DumpMonitor)
     */
    void register(@Nonnull DumpMonitor monitor, @Nonnull DumpTarget target)
            throws InterruptedException, DiagnosticException;

    /**
     * Starts the new transaction.
     * @param monitor the operation monitor
     * @throws InterruptedException if interrupted during the operation
     * @throws DiagnosticException if error was occurred while starting a new transaction
     * @throws IllegalStateException if there are no tables {@link #register(DumpMonitor, DumpTarget) registered}
     * @throws IllegalStateException If the transaction have been already started
     */
    void begin(@Nonnull DumpMonitor monitor) throws InterruptedException, DiagnosticException;

    /**
     * Executes a dump operation for the table.
     * @param monitor the operation monitor
     * @param target the dump target information
     * @throws InterruptedException if interrupted during the operation
     * @throws DiagnosticException if error was occurred
     * @throws IllegalArgumentException if the dump target is not {@link #register(DumpMonitor, DumpTarget) registered}
     * @throws IllegalStateException if the transaction have not been started. or already finished
     */
    void execute(@Nonnull DumpMonitor monitor, @Nonnull DumpTarget target)
            throws InterruptedException, DiagnosticException;

    /**
     * Commits the series of operations, and finishes the current transaction.
     * @param monitor the operation monitor
     * @throws InterruptedException if interrupted during the operation
     * @throws DiagnosticException if error was occurred while committing the current transaction
     * @throws IllegalStateException If the transaction have not been started
     * @throws IllegalStateException If the dump operation have been still running
     */
    void commit(@Nonnull DumpMonitor monitor) throws InterruptedException, DiagnosticException;

    /**
     * Disposes this object.
     * <p>
     * This does nothing if the object is already closed.
     * </p>
     * <p>
     * If the current transaction is still running and not yet committed, this operation will revoke it.
     * </p>
     * @throws InterruptedException if interrupted during the operation
     * @throws DiagnosticException if error was occurred
     */
    @Override
    void close() throws InterruptedException, DiagnosticException;
}
