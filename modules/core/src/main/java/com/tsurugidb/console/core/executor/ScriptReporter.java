package com.tsurugidb.console.core.executor;

import java.text.MessageFormat;
import java.util.List;
import java.util.Optional;

import com.tsurugidb.sql.proto.SqlRequest.CommitStatus;
import com.tsurugidb.sql.proto.SqlRequest.TransactionOption;
import com.tsurugidb.tsubakuro.exception.ServerException;

public abstract class ScriptReporter {

    public abstract void info(String message);

    public abstract void succeed(String message);

    public abstract void warn(String message);

    public void warn(ServerException e) {
        String message = MessageFormat.format("{0} ({1})", //
                e.getDiagnosticCode().name(), //
                e.getMessage());
        warn(message);
    }

    public void reportStartTransaction(TransactionOption option) {
        String message = MessageFormat.format("transaction started. option=[{0}]", //
                option);
        reportStartTransaction(message, option);
    }

    protected void reportStartTransaction(String message, TransactionOption option) {
        succeed(message);
    }

    public void reportCommitTransaction(Optional<CommitStatus> status) {
        String message = MessageFormat.format("transaction committed. status={0}", //
                status.map(CommitStatus::name).orElse("DEFAULT"));
        reportCommitTransaction(message, status);
    }

    protected void reportCommitTransaction(String message, Optional<CommitStatus> status) {
        succeed(message);
    }

    public void reportRollbackTransaction() {
        String message = "transaction rollbacked.";
        reportRollbackTransaction(message);
    }

    protected void reportRollbackTransaction(String message) {
        succeed(message);
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
