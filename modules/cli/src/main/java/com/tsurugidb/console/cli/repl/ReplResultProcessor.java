package com.tsurugidb.console.cli.repl;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.Nonnull;

import com.tsurugidb.console.core.executor.result.ResultProcessor;
import com.tsurugidb.console.core.executor.result.ResultSetUtil;
import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.ResultSetMetadata;

/**
 * Tsurugi SQL console repl ResultProcessor.
 */
public class ReplResultProcessor implements ResultProcessor {

    private final ReplReporter reporter;

    /**
     * Creates a new instance.
     * 
     * @param reporter ReplReporter
     */
    public ReplResultProcessor(@Nonnull ReplReporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void process(ResultSet target) throws ServerException, IOException, InterruptedException {
        dumpMetadata(target.getMetadata());

        var list = new ArrayList<Object>();
        int rowSize = 0;
        while (ResultSetUtil.fetchNextRow(target, target.getMetadata(), list::add)) {
            reporter.reportResultSetRow(list.toString());
            rowSize++;
            list.clear();
        }

        reporter.reportResultSetSize(rowSize);
    }

    private void dumpMetadata(ResultSetMetadata metadata) throws IOException {
        var columns = metadata.getColumns();
        var list = new ArrayList<Field>(columns.size());
        for (int i = 0, n = columns.size(); i < n; i++) {
            list.add(new Field(i, columns.get(i)));
        }
        reporter.reportResultSetHeader(list.toString());
    }

    private static class Field {
        private int index;
        private SqlCommon.Column column;

        Field(int index, SqlCommon.Column column) {
            this.index = index;
            this.column = column;
        }

        public String getName() {
            return ResultSetUtil.getFieldName(column, index);
        }

        public String getType() {
            return getFieldType(column);
        }

        @Override
        public String toString() {
            return getName() + ": " + getType();
        }
    }

    private static String getFieldType(SqlCommon.Column column) {
        switch (column.getTypeInfoCase()) {
        case ATOM_TYPE:
            return column.getAtomType().name();
        case ROW_TYPE:
            return column.getRowType().getColumnsList().toString();
        case USER_TYPE:
            return column.getUserType().getName();
        case TYPEINFO_NOT_SET:
        default:
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }
}
