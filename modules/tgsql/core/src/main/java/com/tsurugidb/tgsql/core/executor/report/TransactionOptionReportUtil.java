package com.tsurugidb.tgsql.core.executor.report;

import java.util.List;
import java.util.stream.Collectors;

import com.tsurugidb.sql.proto.SqlRequest.ReadArea;
import com.tsurugidb.sql.proto.SqlRequest.TransactionOption;
import com.tsurugidb.sql.proto.SqlRequest.TransactionPriority;
import com.tsurugidb.sql.proto.SqlRequest.TransactionType;
import com.tsurugidb.sql.proto.SqlRequest.WritePreserve;

/**
 * transaction option report utility.
 */
public class TransactionOptionReportUtil {

    private static TransactionOptionReportUtil defaultInstance = new TransactionOptionReportUtil();

    /**
     * get default instance.
     *
     * @return transaction option report utility
     */
    public static TransactionOptionReportUtil getInstance() {
        return defaultInstance;
    }

    /**
     * set default instance.
     *
     * @param instance transaction option report utility
     */
    public static void setDefaultInstance(TransactionOptionReportUtil instance) {
        defaultInstance = instance;
    }

    /**
     * convert text.
     *
     * @param option transaction option
     * @return text
     */
    public String toString(TransactionOption option) {
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

    /**
     * convert text.
     *
     * @param type transaction type
     * @return text
     */
    public String toString(TransactionType type) {
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

    /**
     * convert text.
     *
     * @param list write preserve
     * @return text
     */
    public String toStringWp(List<WritePreserve> list) {
        return list.stream().map(s -> "\"" + s.getTableName() + "\"").collect(Collectors.joining(", "));
    }

    /**
     * convert text.
     *
     * @param list read area
     * @return text
     */
    public String toStringRa(List<ReadArea> list) {
        return list.stream().map(s -> "\"" + s.getTableName() + "\"").collect(Collectors.joining(", "));
    }

    /**
     * convert text.
     *
     * @param priority transaction priority
     * @return text
     */
    public String toString(TransactionPriority priority) {
        switch (priority) {
        case TRANSACTION_PRIORITY_UNSPECIFIED:
            return "unspecified";
        case WAIT:
            return "prior deferrable";
        case INTERRUPT:
            return "prior immediate";
        case WAIT_EXCLUDE:
            return "excluding deferrable";
        case INTERRUPT_EXCLUDE:
            return "excluding immediate";
        default:
            return priority.toString().toLowerCase();
        }
    }
}
