package com.tsurugidb.console.core.executor.report;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import com.tsurugidb.sql.proto.SqlRequest.CommitStatus;
import com.tsurugidb.sql.proto.SqlRequest.TransactionOption;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * reporter of Tsurugi SQL console.
 */
public abstract class ScriptReporter {

    /**
     * output information message.
     *
     * @param message message
     */
    public abstract void info(String message);

    /**
     * output implicit message.
     *
     * @param message message
     */
    public abstract void implicit(String message);

    /**
     * output succeed message.
     *
     * @param message message
     */
    public abstract void succeed(String message);

    /**
     * output warning message.
     *
     * @param message message
     */
    public abstract void warn(String message);

    /**
     * output warning message.
     *
     * @param e ServerException
     */
    public void warn(ServerException e) {
        String message = MessageFormat.format("{0} ({1})", //
                e.getDiagnosticCode().name(), //
                e.getMessage());
        warn(message);
    }

    /**
     * output message for start transaction implicitly.
     *
     * @param option transaction option
     */
    public void reportStartTransactionImplicitly(TransactionOption option) {
        String message = MessageFormat.format("start transaction implicitly. option=[{0}]", //
                option);
        reportStartTransactionImplicitly(message, option);
    }

    /**
     * output message for start transaction implicitly.
     *
     * @param message message
     * @param option  transaction option
     */
    protected void reportStartTransactionImplicitly(String message, TransactionOption option) {
        implicit(message);
    }

    /**
     * output message for transaction started.
     *
     * @param option transaction option
     */
    public void reportTransactionStarted(TransactionOption option) {
        String message = MessageFormat.format("transaction started. option=[{0}]", //
                option);
        reportTransactionStarted(message, option);
    }

    /**
     * output message for transaction started.
     *
     * @param message message
     * @param option  transaction option
     */
    protected void reportTransactionStarted(String message, TransactionOption option) {
        succeed(message);
    }

    /**
     * output message for transaction committed.
     *
     * @param status commit status
     */
    public void reportTransactionCommitted(Optional<CommitStatus> status) {
        String message = MessageFormat.format("transaction committed. status={0}", //
                status.map(CommitStatus::name).orElse("DEFAULT"));
        reportTransactionCommitted(message, status);
    }

    /**
     * output message for transaction committed.
     *
     * @param message message
     * @param status  commit status
     */
    protected void reportTransactionCommitted(String message, Optional<CommitStatus> status) {
        succeed(message);
    }

    /**
     * output message for transaction committed implicitly.
     *
     * @param status commit status
     */
    public void reportTransactionCommittedImplicitly(CommitStatus status) {
        String message = MessageFormat.format("transaction committed implicitly. status={0}", //
                status);
        reportTransactionCommittedImplicitly(message, status);
    }

    /**
     * output message for transaction committed implicitly.
     *
     * @param message message
     * @param status  commit status
     */
    protected void reportTransactionCommittedImplicitly(String message, CommitStatus status) {
        implicit(message);
    }

    /**
     * output message for transaction rollbacked.
     */
    public void reportTransactionRollbacked() {
        String message = "transaction rollbacked.";
        reportTransactionRollbacked(message);
    }

    /**
     * output message for transaction rollbacked.
     *
     * @param message message
     */
    protected void reportTransactionRollbacked(String message) {
        succeed(message);
    }

    /**
     * output message for transaction rollbacked implicitly.
     */
    public void reportTransactionRollbackedImplicitly() {
        String message = "transaction rollbacked implicitly.";
        reportTransactionRollbackedImplicitly(message);
    }

    /**
     * output message for transaction rollbacked implicitly.
     *
     * @param message message
     */
    protected void reportTransactionRollbackedImplicitly(String message) {
        implicit(message);
    }

    /**
     * output message for transaction status.
     *
     * @param active {@code true} if transaction is active
     */
    public void reportTransactionStatus(boolean active) {
        String message = MessageFormat.format("transaction is {0}", //
                active ? "active" : "inactive");
        reportTransactionStatus(message, active);
    }

    /**
     * output message for transaction status.
     *
     * @param message message
     * @param active  {@code true} if transaction is active
     */
    protected void reportTransactionStatus(String message, boolean active) {
        info(message);
    }

    /**
     * output message for help.
     *
     * @param list message list
     */
    public void reportHelp(List<String> list) {
        for (var s : list) {
            info(s);
        }
    }
}
