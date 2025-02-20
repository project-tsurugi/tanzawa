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
package com.tsurugidb.tools.tgdump.core.engine;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;
import com.tsurugidb.tools.tgdump.core.model.DumpProfile;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;
import com.tsurugidb.tools.tgdump.core.model.TransactionSettings;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;

/**
 * A basic implementation of {@link DumpSession}.
 */
public class BasicDumpSession implements DumpSession {

    /**
     * The session state.
     */
    enum State {

        /**
         * Preparing the dump targets.
         */
        PREPARING,

        /**
         * Starting the dump operations.
         */
        STARTING,

        /**
         * Running the dump operations.
         */
        RUNNING,

        /**
         * Committing the dump operations.
         */
        COMMITTING,

        /**
         * Committed the dump operations.
         */
        COMMITTED,

        /**
         * Operation was failed.
         */
        FAILED,

        /**
         * Aborted or Not started the dump operations.
         */
        CLOSED,
    }

    private static final Logger LOG = LoggerFactory.getLogger(BasicDumpSession.class);

    private final SqlClient client;

    private final TransactionSettings transactionSettings;

    private final DumpOperationDispatch operation;

    private final AtomicReference<State> stateRef = new AtomicReference<>(State.PREPARING);

    private final AtomicReference<Transaction> transactionRef = new AtomicReference<>();

    /**
     * Creates a new instance.
     * <p>
     * This will invoke {@link SqlClient#close()} during this object is closed.
     * </p>
     * @param client the SQL client to execute the series of operations
     * @param transactionSettings the transaction settings for dump operations
     * @param dumpProfile the dump operation settings
     * @param createTargetDirectories whether or not to create dump target directories before the dump operations
     */
    public BasicDumpSession(
            @Nonnull SqlClient client,
            @Nonnull TransactionSettings transactionSettings,
            @Nonnull DumpProfile dumpProfile,
            boolean createTargetDirectories) {
        this(client, transactionSettings, createOperationsMap(dumpProfile, createTargetDirectories));
    }

    private static @Nonnull Map<DumpTarget.TargetType, DumpOperation> createOperationsMap(
            @Nonnull DumpProfile dumpProfile,
            boolean createTargetDirectories) {
        Objects.requireNonNull(dumpProfile);
        return Map.ofEntries(
            Map.entry(DumpTarget.TargetType.TABLE, new TableDumpOperation(dumpProfile, createTargetDirectories)),
            Map.entry(DumpTarget.TargetType.QUERY, new QueryDumpOperation(dumpProfile, createTargetDirectories)));
    }

    /**
     * Creates a new instance.
     * <p>
     * This will invoke {@link SqlClient#close()} during this object is closed.
     * </p>
     * <p>
     * This is designed for testing mainly.
     * </p>
     * @param client the SQL client to execute the series of operations
     * @param transactionSettings the transaction settings for dump operations
     * @param operations the dump operations
     */
    BasicDumpSession(
            @Nonnull SqlClient client,
            @Nonnull TransactionSettings transactionSettings,
            @Nonnull Map<DumpTarget.TargetType, DumpOperation> operations) {
        Objects.requireNonNull(client);
        Objects.requireNonNull(transactionSettings);
        Objects.requireNonNull(operations);
        this.client = client;
        this.transactionSettings = transactionSettings;
        this.operation = new DumpOperationDispatch(operations);
    }

    /**
     * Returns the current session state.
     *
     * <p>
     * This is designed only for testing.
     * </p>
     * @return the current session state
     */
    State getState() {
        return stateRef.get();
    }

    // PREPARING -> PREPARING
    @Override
    public void register(@Nonnull DumpMonitor monitor, @Nonnull DumpTarget target)
            throws InterruptedException, DiagnosticException {
        Objects.requireNonNull(monitor);
        Objects.requireNonNull(target);

        LOG.trace("enter: register: {}", target); //$NON-NLS-1$
        if (stateRef.get() != State.PREPARING) {
            throw new IllegalStateException(MessageFormat.format(
                    "inconsistent operation state: {0} (expected: {1})",
                    stateRef.get(),
                    State.PREPARING));
        }
        operation.register(client, monitor, target);
        LOG.trace("exit: register: {}", target); //$NON-NLS-1$
    }

    // PREPARING -> STARTING -> (RUNNING|FAILED)
    @Override
    public void begin(@Nonnull DumpMonitor monitor) throws InterruptedException, DiagnosticException {
        Objects.requireNonNull(monitor);
        LOG.trace("enter: begin"); //$NON-NLS-1$
        if (!stateRef.compareAndSet(State.PREPARING, State.STARTING)) {
            throw new IllegalStateException(MessageFormat.format(
                    "inconsistent operation state: {0} (expected: {1})",
                    stateRef.get(),
                    State.PREPARING));
        }
        boolean complete = false;
        try {
            if (operation.isEmpty()) {
                throw new IllegalStateException("dump targets are empty");
            }
            var tables = operation.getTargetTables();
            var txOptions = transactionSettings.toProtocolBuffer(tables);
            monitor.verbose("starting a new transaction: {0}", txOptions); //$NON-NLS-1$
            try {
                var transaction = client.createTransaction(txOptions).await();
                transactionRef.set(transaction); // TODO: check existing
                monitor.verbose("transaction was started: {0}", transaction.getTransactionId()); //$NON-NLS-1$
            } catch (IOException e) {
                LOG.debug("exception was occurred in begin", e); //$NON-NLS-1$
                throw new DumpException(DumpDiagnosticCode.IO_ERROR, List.of(e.toString()), e);
            } catch (ServerException e) {
                LOG.debug("exception was occurred in begin", e); //$NON-NLS-1$
                throw new DumpException(DumpDiagnosticCode.BEGIN_FAILURE, List.of(), e);
            }
            complete = true;
        } finally {
            stateRef.compareAndSet(State.STARTING, complete ? State.RUNNING : State.FAILED);
        }
        LOG.trace("exit: begin"); //$NON-NLS-1$
    }

    // RUNNING -> RUNNING
    @Override
    public void execute(@Nonnull DumpMonitor monitor, @Nonnull DumpTarget target)
            throws InterruptedException, DiagnosticException {
        Objects.requireNonNull(monitor);
        Objects.requireNonNull(target);
        LOG.trace("enter: execute: {}", target); //$NON-NLS-1$
        if (stateRef.get() != State.RUNNING) {
            throw new IllegalStateException(MessageFormat.format(
                    "inconsistent operation state: {0} (expected: {1})",
                    stateRef.get(),
                    State.RUNNING));
        }
        var transaction = transactionRef.get();
        if (transaction == null) {
            // may not occur in general cases
            throw new IllegalStateException("transaction object is missing");
        }
        operation.execute(client, transaction, monitor, target);
        LOG.trace("exit: execute: {}", target); //$NON-NLS-1$
    }

    // RUNNING -> COMMITTING -> (COMMITTED|FAILED)
    @Override
    public void commit(@Nonnull DumpMonitor monitor) throws InterruptedException, DiagnosticException {
        Objects.requireNonNull(monitor);
        LOG.trace("enter: commit"); //$NON-NLS-1$
        if (!stateRef.compareAndSet(State.RUNNING, State.COMMITTING)) {
            throw new IllegalStateException(MessageFormat.format(
                    "inconsistent operation state: {0} (expected: {1})",
                    stateRef.get(),
                    State.PREPARING));
        }
        boolean complete = false;
        try {
            var transaction = transactionRef.get();
            if (transaction == null) {
                // may not occur
                throw new IllegalStateException("transaction object is missing");
            }
            monitor.verbose("committing the current transaction: {0}", transaction.getTransactionId()); //$NON-NLS-1$
            try {
                transaction.commit().await();
            } catch (IOException e) {
                LOG.debug("exception was occurred in commit", e); //$NON-NLS-1$
                throw new DumpException(DumpDiagnosticCode.IO_ERROR, List.of(e.toString()), e);
            } catch (ServerException e) {
                LOG.debug("exception was occurred in commit", e); //$NON-NLS-1$
                throw new DumpException(DumpDiagnosticCode.COMMIT_FAILURE, List.of(), e);
            }
            monitor.verbose("transaction was committed: {0}", transaction.getTransactionId()); //$NON-NLS-1$
            complete = true;
        } finally {
            stateRef.compareAndSet(State.COMMITTING, complete ? State.COMMITTED : State.FAILED);
        }
        LOG.trace("exit: commit"); //$NON-NLS-1$
    }

    // * -> CLOSED
    @Override
    public void close() throws InterruptedException, DiagnosticException {
        LOG.trace("enter: close"); //$NON-NLS-1$
        while (true) {
            State state = stateRef.get();
            if (stateRef.get() == State.CLOSED) {
                LOG.trace("exit: close (already closed)"); //$NON-NLS-1$
                return; // already closed
            }
            if (stateRef.compareAndSet(state, State.CLOSED)) {
                break;
            }
        }
        try (
            var transaction = transactionRef.get();
            var c = client
        ) {
            LOG.debug("closing dump session"); //$NON-NLS-1$
        } catch (IOException e) {
            LOG.debug("exception was occurred in close", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.IO_ERROR, List.of(e.toString()), e);
        } catch (ServerException e) {
            LOG.debug("exception was occurred in close", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.SERVER_ERROR, List.of(DiagnosticUtil.getMessage(e)), e);
        }
        LOG.trace("exit: close"); //$NON-NLS-1$
    }
}
