package com.tsurugidb.tgsql.cli.repl;

import java.io.IOException;
import java.util.ArrayList;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.config.TgsqlCvKey;
import com.tsurugidb.tgsql.core.executor.result.ResultProcessor;
import com.tsurugidb.tgsql.core.executor.result.ResultSetUtil;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.ResultSetMetadata;

/**
 * Tsurugi SQL console repl ResultProcessor.
 */
public class ReplResultProcessor implements ResultProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(ReplResultProcessor.class);

    private final TgsqlConfig config;
    private final ReplReporter reporter;

    /**
     * Creates a new instance.
     *
     * @param config   tgsql configuration
     * @param reporter ReplReporter
     */
    public ReplResultProcessor(@Nonnull TgsqlConfig config, @Nonnull ReplReporter reporter) {
        this.config = config;
        this.reporter = reporter;
    }

    @Override
    public long process(ResultSet target) throws ServerException, IOException, InterruptedException {
        dumpMetadata(target.getMetadata());
        if (Thread.interrupted()) {
            LOG.trace("Thread.interrupted (1)");
            throw new InterruptedException();
        }

        var clientVariableMap = config.getClientVariableMap();
        int maxLines = clientVariableMap.get(TgsqlCvKey.SELECT_MAX_LINES, -1);
        boolean over = false;

        var columnList = new ArrayList<Object>();
        int rowSize = 0;
        while (ResultSetUtil.fetchNextRow(target, target.getMetadata(), columnList::add)) {
            if (maxLines >= 0) {
                if (rowSize >= maxLines) {
                    over = true;
                    break;
                }
            }

            reporter.reportResultSetRow(columnList.toString());
            rowSize++;
            columnList.clear();

            if (Thread.interrupted()) {
                LOG.trace("Thread.interrupted (2)");
                throw new InterruptedException();
            }
        }
        long timingEnd = System.nanoTime();

        if (over) {
            reporter.reportResultSetRow("...");
        }
        reporter.reportResultSetSize(rowSize, over);

        return timingEnd;
    }

    private void dumpMetadata(ResultSetMetadata metadata) throws IOException {
        var columns = metadata.getColumns();
        var list = new ArrayList<Field>(columns.size());
        for (int i = 0, n = columns.size(); i < n; i++) {
            list.add(new Field(i, columns.get(i)));
        }
        reporter.reportResultSetHeader(list.toString());
    }

    private class Field {
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
            return reporter.getFieldTypeText(column);
        }

        @Override
        public String toString() {
            return getName() + ": " + getType();
        }
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }
}
