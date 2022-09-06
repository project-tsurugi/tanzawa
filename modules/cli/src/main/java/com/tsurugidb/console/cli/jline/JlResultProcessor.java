package com.tsurugidb.console.cli.jline;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import org.jline.terminal.Terminal;

import com.tsurugidb.console.core.executor.ResultProcessor;
import com.tsurugidb.console.core.executor.ResultSetUtil;
import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.ResultSetMetadata;

public class JlResultProcessor implements ResultProcessor {

    private static final String FIELD_NAME_PREFIX_UNNAMED = "@#";

    private final PrintWriter writer;

    public JlResultProcessor(Terminal terminal) {
        this.writer = terminal.writer();
    }

    @Override
    public void process(ResultSet target) throws ServerException, IOException, InterruptedException {
        dumpMetadata(target.getMetadata());

        var list = new ArrayList<Object>();
        while (ResultSetUtil.fetchNextRow(target, target.getMetadata(), list::add)) {
            writer.println(list);
            list.clear();
        }
    }

    private void dumpMetadata(ResultSetMetadata metadata) throws IOException {
        var columns = metadata.getColumns();
        var list = new ArrayList<Field>(columns.size());
        for (int i = 0, n = columns.size(); i < n; i++) {
            list.add(new Field(i, columns.get(i)));
        }
        writer.println(list);
    }

    private static class Field {
        private int index;
        private SqlCommon.Column column;

        Field(int index, SqlCommon.Column column) {
            this.index = index;
            this.column = column;
        }

        public String getName() {
            return getFieldName(column, index);
        }

        public String getType() {
            return getFieldType(column);
        }

        @Override
        public String toString() {
            return getName() + ": " + getType();
        }
    }

    private static String getFieldName(SqlCommon.Column column, int index) {
        if (column.getName().isEmpty()) {
            return String.format("%s%d", FIELD_NAME_PREFIX_UNNAMED, index);
        }
        return column.getName();
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
