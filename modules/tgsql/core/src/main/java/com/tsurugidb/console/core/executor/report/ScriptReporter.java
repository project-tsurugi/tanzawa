package com.tsurugidb.console.core.executor.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.sql.proto.SqlRequest.CommitStatus;
import com.tsurugidb.sql.proto.SqlRequest.ReadArea;
import com.tsurugidb.sql.proto.SqlRequest.TransactionOption;
import com.tsurugidb.sql.proto.SqlRequest.TransactionPriority;
import com.tsurugidb.sql.proto.SqlRequest.TransactionType;
import com.tsurugidb.sql.proto.SqlRequest.WritePreserve;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.explain.PlanGraph;
import com.tsurugidb.tsubakuro.sql.CounterType;
import com.tsurugidb.tsubakuro.sql.ExecuteResult;
import com.tsurugidb.tsubakuro.sql.SqlServiceException;
import com.tsurugidb.tsubakuro.sql.TableMetadata;

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

    //

    /**
     * output message for connect.
     *
     * @param endpoint endpoint
     */
    public void reportConnect(String endpoint) {
        String message = MessageFormat.format("connected {0}", endpoint);
        succeed(message);
    }

    /**
     * output message for disconnect.
     *
     * @param disconnected {@code false} if already disconnected
     */
    public void reportDisconnect(boolean disconnected) {
        if (disconnected) {
            succeed("disconnected");
        } else {
            succeed("already disconnected");
        }
    }

    /**
     * output message for start transaction implicitly.
     *
     * @param option transaction option
     */
    public void reportStartTransactionImplicitly(TransactionOption option) {
        String message = MessageFormat.format("start transaction implicitly. option={0}", //
                toString(option));
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
        String message = MessageFormat.format("transaction started. option={0}", //
                toString(option));
        reportTransactionStarted(message, option);
    }

    protected String toString(TransactionOption option) {
        var sb = new StringBuilder(64);

        {
            sb.append("[\n  type: ");
            sb.append(toString(option.getType()));
        }
        {
            var label = option.getLabel();
            if (label != null && !label.isEmpty()) {
                sb.append("\n  label: \"");
                sb.append(label);
                sb.append("\"");
            }
        }
        {
            var list = option.getWritePreservesList();
            if (!list.isEmpty()) {
                sb.append("\n  write_preserve: ");
                sb.append(toStringWp(list));
            }
        }
        {
            var includeDdl = option.getModifiesDefinitions();
            if (includeDdl) {
                sb.append("\n  include_ddl: ");
                sb.append(includeDdl);
            }
        }
        {
            var list = option.getInclusiveReadAreasList();
            if (!list.isEmpty()) {
                sb.append("\n  read_area_include: ");
                sb.append(toStringRa(list));
            }
        }
        {
            var list = option.getExclusiveReadAreasList();
            if (!list.isEmpty()) {
                sb.append("\n  read_area_exclude: ");
                sb.append(toStringRa(list));
            }
        }
        {
            var priority = option.getPriority();
            if (priority != null && priority != TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED) {
                sb.append("\n  priority: ");
                sb.append(toString(priority));
            }
        }

        sb.append("\n]");
        return sb.toString();
    }

    protected String toString(TransactionType type) {
        switch (type) {
        case SHORT:
            return "OCC";
        case LONG:
            return "LTX";
        case READ_ONLY:
            return "RTX";
        case TRANSACTION_TYPE_UNSPECIFIED:
            return "DEFAULT";
        default:
            return type.toString();
        }
    }

    protected String toStringWp(List<WritePreserve> list) {
        return list.stream().map(s -> "\"" + s.getTableName() + "\"").collect(Collectors.joining(", "));
    }

    protected String toStringRa(List<ReadArea> list) {
        return list.stream().map(s -> "\"" + s.getTableName() + "\"").collect(Collectors.joining(", "));
    }

    protected String toString(TransactionPriority priority) {
        switch (priority) {
        case WAIT:
            return "prior deferrable";
        case INTERRUPT:
            return "prior immediate";
        case WAIT_EXCLUDE:
            return "excluding deferrable";
        case INTERRUPT_EXCLUDE:
            return "excluding immediate";
        default:
            return priority.toString();
        }
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
        String message = MessageFormat.format("transaction commit({0}) finished.", //
                getCommitStatusMessage(status.orElse(null)));
        reportTransactionCommitted(message, status);
    }

    protected String getCommitStatusMessage(CommitStatus status) {
        if (status == null || status == CommitStatus.COMMIT_STATUS_UNSPECIFIED) {
            return "DEFAULT";
        }
        return status.name();
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
        String message = MessageFormat.format("transaction commit({0}) finished implicitly.", //
                getCommitStatusMessage(status));
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
        String message = "transaction rollback finished.";
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
        String message = "transaction rollback finished implicitly.";
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
     * output message for transaction close implicitly.
     */
    public void reportTransactionCloseImplicitly() {
        implicit("transaction closed.");
    }

    /**
     * output message for session status.
     *
     * @param endpoint endpoint
     * @param active   {@code true} if session is active
     */
    public void reportSessionStatus(String endpoint, boolean active) {
        String activeMessage = active ? "active" : "inactive";

        String message;
        if (endpoint != null) {
            message = MessageFormat.format("session({0}) is {1}", endpoint, activeMessage);
        } else {
            message = MessageFormat.format("session is {0}", activeMessage);
        }

        reportSessionStatus(message, endpoint, active);
    }

    /**
     * output message for session status.
     *
     * @param message  message
     * @param endpoint endpoint
     * @param active   {@code true} if session is active
     */
    protected void reportSessionStatus(String message, String endpoint, boolean active) {
        info(message);
    }

    /**
     * output message for transaction status.
     *
     * @param active        {@code true} if transaction is active
     * @param transactionId transaction id
     */
    public void reportTransactionStatus(boolean active, String transactionId) {
        String message;
        if (active) {
            message = MessageFormat.format("transaction is active. transactionId={0}", //
                    transactionId);
        } else {
            message = "transaction is inactive";
        }
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
     * output message for transaction exception.
     *
     * @param exception transaction exception
     */
    public void reportTransactionException(SqlServiceException exception) {
        String message;
        if (exception == null) {
            message = "tranasction status: normal";
        } else {
            message = "transaction status: cannot continue (" + exception.getMessage() + ")";
        }
        reportTransactionException(message, exception);
    }

    /**
     * output message for transaction exception.
     *
     * @param message   message
     * @param exception transaction exception
     */
    protected void reportTransactionException(String message, SqlServiceException exception) {
        if (exception == null) {
            info(message);
        } else {
            warn(message);
        }
    }

    /**
     * output message for result of statement.
     *
     * @param result execute result
     */
    public void reportStatementResult(ExecuteResult result) {
        var counterMap = result.getCounters();
        if (counterMap.isEmpty()) {
            reportStatementResult();
            return;
        }
        var sb = new StringBuilder();
        sb.append("(");
        for (var entry : counterMap.entrySet()) {
            if (sb.charAt(sb.length() - 1) != '(') {
                sb.append(", ");
            }
            sb.append(getStatementResultMessage(entry.getKey(), entry.getValue()));
        }
        sb.append(")");

        succeed(sb.toString());
    }

    /**
     * get message for result of statement.
     *
     * @param counterType counter type
     * @param count       rows
     * @return message
     */
    protected String getStatementResultMessage(CounterType counterType, long count) {
        switch (counterType) {
        case INSERTED_ROWS:
            return MessageFormat.format("{0,choice,0#0 rows|1#1 row|1<{0} rows} inserted", count);
        case UPDATED_ROWS:
            return MessageFormat.format("{0,choice,0#0 rows|1#1 row|1<{0} rows} updated", count);
        case MERGED_ROWS:
            return MessageFormat.format("{0,choice,0#0 rows|1#1 row|1<{0} rows} merged", count);
        case DELETED_ROWS:
            return MessageFormat.format("{0,choice,0#0 rows|1#1 row|1<{0} rows} deleted", count);
        default:
            break;
        }

        if (count == 1) {
            return "1 row " + getCounterName(counterType); //$NON-NLS-1$
        } else {
            return count + " rows " + getCounterName(counterType); //$NON-NLS-1$
        }
    }

    /**
     * get counter name.
     *
     * @param counterType counter type
     * @return counter name
     */
    protected String getCounterName(CounterType counterType) {
        String name = counterType.name().toLowerCase(Locale.ENGLISH);
        String suffix = "_rows";
        if (name.endsWith(suffix)) {
            name = name.substring(0, name.length() - suffix.length());
        }
        return name;
    }

    /**
     * output message for result of statement.
     */
    public void reportStatementResult() {
        succeed("execute succeeded");
    }

    /**
     * output message for table list.
     *
     * @param tableList table names
     */
    public void reportTableList(List<String> tableList) {
        for (var name : tableList) {
            info(name);
        }
    }

    /**
     * output message for table metadata.
     *
     * @param specifiedTableName table name
     * @param data               metadata
     */
    public void reportTableMetadata(String specifiedTableName, @Nullable TableMetadata data) {
        if (data == null) {
            String message = MessageFormat.format("''{0}'' table not found", specifiedTableName);
            warn(message);
            return;
        }

        String databaseName = data.getDatabaseName().orElse(null);
        reportTableMetadata("databaseName", databaseName);
        String schemaName = data.getSchemaName().orElse(null);
        reportTableMetadata("schemaName", schemaName);
        String tableName = data.getTableName();
        reportTableMetadata("tableName", tableName);

        int i = 0;
        for (var column : data.getColumns()) {
            reportTableMetadata(column, i++);
        }
    }

    protected void reportTableMetadata(String title, String name) {
        String message;
        if (name != null) {
            message = MessageFormat.format("{0}=''{1}''", title, name);
        } else {
            message = MessageFormat.format("{0}=null", title);
        }
        info(message);
    }

    protected void reportTableMetadata(SqlCommon.Column column, int index) {
        String name = column.getName();
        String type = getFieldTypeText(column);
        String message = MessageFormat.format("({0}) {1}: {2}", index, name, type);
        info(message);
    }

    /**
     * get field type.
     *
     * @param column Column
     * @return type text
     */
    public String getFieldTypeText(SqlCommon.Column column) {
        switch (column.getTypeInfoCase()) {
        case ATOM_TYPE:
            return column.getAtomType().name();
        case ROW_TYPE:
            return column.getRowType().getColumnsList().stream().map(this::getFieldTypeText).collect(Collectors.joining(", ", "[", "]"));
        case USER_TYPE:
            return column.getUserType().getName();
        case TYPEINFO_NOT_SET:
        default:
            return "";
        }
    }

    private PlanGraphReporter planGraphReporter;

    /**
     * displays execution plan.
     *
     * @param source the source program
     * @param plan   the inspected plan
     */
    public void reportExecutionPlan(@Nonnull String source, @Nonnull PlanGraph plan) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(plan);

        if (this.planGraphReporter == null) {
            this.planGraphReporter = new PlanGraphReporter(message -> info(message));
        }
        planGraphReporter.report(source, plan);
    }

    /**
     * output message for command history.
     *
     * @param iterator command history
     */
    public void reportHistory(Iterator<HistoryEntry> iterator) {
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String message = MessageFormat.format("{0} {1}", entry.index(), entry.text());
            info(message);
        }
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

    /**
     * output message for timing.
     *
     * @param nanoTime time
     */
    public void reportTiming(long nanoTime) {
        var time = BigDecimal.valueOf(nanoTime).divide(BigDecimal.valueOf(1000_000), 3, RoundingMode.DOWN);
        String message = MessageFormat.format("Time: {0} ms", time);
        implicit(message);
    }
}
