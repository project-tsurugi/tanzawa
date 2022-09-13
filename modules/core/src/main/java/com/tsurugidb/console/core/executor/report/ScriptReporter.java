package com.tsurugidb.console.core.executor.report;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import com.tsurugidb.console.core.executor.engine.ExecutorUtil;
import com.tsurugidb.sql.proto.SqlRequest.CommitStatus;
import com.tsurugidb.sql.proto.SqlRequest.TransactionOption;
import com.tsurugidb.tsubakuro.exception.ServerException;

public abstract class ScriptReporter {

    public abstract void info(String message);

    public abstract void implicit(String message);

    public abstract void succeed(String message);

    public abstract void warn(String message);

    public void warn(ServerException e) {
        String message = MessageFormat.format("{0} ({1})", //
                e.getDiagnosticCode().name(), //
                e.getMessage());
        warn(message);
    }

    public void reportStartTransactionImplicitly(TransactionOption option) {
        String message = MessageFormat.format("start transaction implicitly. option=[{0}]", //
                option);
        reportStartTransactionImplicitly(message, option);
    }

    protected void reportStartTransactionImplicitly(String message, TransactionOption option) {
        implicit(message);
    }

    public void reportTransactionStarted(TransactionOption option) {
        String message = MessageFormat.format("transaction started. option=[{0}]", //
                option);
        reportTransactionStarted(message, option);
    }

    protected void reportTransactionStarted(String message, TransactionOption option) {
        succeed(message);
    }

    public void reportTransactionCommitted(Optional<CommitStatus> status) {
        String message = MessageFormat.format("transaction committed. status={0}", //
                status.map(CommitStatus::name).orElse("DEFAULT"));
        reportTransactionCommitted(message, status);
    }

    protected void reportTransactionCommitted(String message, Optional<CommitStatus> status) {
        succeed(message);
    }

    public void reportTransactionCommittedImplicitly(CommitStatus status) {
        String message = MessageFormat.format("transaction committed implicitly. status={0}", //
                status);
        reportTransactionCommittedImplicitly(message, status);
    }

    protected void reportTransactionCommittedImplicitly(String message, CommitStatus status) {
        implicit(message);
    }

    public void reportTransactionRollbacked() {
        String message = "transaction rollbacked.";
        reportTransactionRollbacked(message);
    }

    protected void reportTransactionRollbacked(String message) {
        succeed(message);
    }

    public void reportTransactionRollbackedImplicitly() {
        String message = "transaction rollbacked implicitly.";
        reportTransactionRollbackedImplicitly(message);
    }

    protected void reportTransactionRollbackedImplicitly(String message) {
        implicit(message);
    }

    public void reportTransactionStatus(boolean active) {
        String message = MessageFormat.format("transaction is {0}", //
                active ? "active" : "inactive");
        reportTransactionStatus(message, active);
    }

    protected void reportTransactionStatus(String message, boolean active) {
        info(message);
    }

    public void reportHelp() {
        List<String> list = ExecutorUtil.getHelpMessage();
        reportHelp(list);
    }

    protected void reportHelp(List<String> list) {
        for (var s : list) {
            info(s);
        }
    }
}
