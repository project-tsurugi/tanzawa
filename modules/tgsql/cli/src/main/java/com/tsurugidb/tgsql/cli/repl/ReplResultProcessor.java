/*
 * Copyright 2023-2024 Project Tsurugi.
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
package com.tsurugidb.tgsql.cli.repl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

    // for test
    ReplResultProcessor() {
        this.config = null;
        this.reporter = null;
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
        var sb = new StringBuilder();
        int rowSize = 0;
        while (ResultSetUtil.fetchNextRow(target, target.getMetadata(), columnList::add)) {
            if (maxLines >= 0) {
                if (rowSize >= maxLines) {
                    over = true;
                    break;
                }
            }

            appendTo(sb, columnList);
            reporter.reportResultSetRow(sb.toString());
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

    private void appendTo(StringBuilder sb, List<Object> columnList) {
        sb.setLength(0);
        sb.append('[');

        int i = 0;
        for (Object value : columnList) {
            if (i++ != 0) {
                sb.append(", ");
            }
            appendTo(sb, value);
        }

        sb.append(']');
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
    private static final DateTimeFormatter OFFSET_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSSXXX");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSSSSS");
    private static final DateTimeFormatter OFFSET_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSSSSSXXX");

    void appendTo(StringBuilder sb, Object value) {
        if (value instanceof BigDecimal) {
            var v = (BigDecimal) value;
            sb.append(v.toPlainString());
            return;
        }

        if (value instanceof LocalDate) {
            var v = (LocalDate) value;
            sb.append(DATE_FORMATTER.format(v));
            return;
        }
        if (value instanceof LocalDateTime) {
            var v = (LocalDateTime) value;
            sb.append(DATETIME_FORMATTER.format(v));
            return;
        }
        if (value instanceof OffsetDateTime) {
            var v = (OffsetDateTime) value;
            sb.append(OFFSET_DATETIME_FORMATTER.format(v));
            return;
        }
        if (value instanceof LocalTime) {
            var v = (LocalTime) value;
            sb.append(TIME_FORMATTER.format(v));
            return;
        }
        if (value instanceof OffsetTime) {
            var v = (OffsetTime) value;
            sb.append(OFFSET_TIME_FORMATTER.format(v));
            return;
        }

        if (value instanceof byte[]) {
            for (byte v : (byte[]) value) {
                sb.append(String.format("%02x", v));
            }
            return;
        }

        sb.append(value);
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }
}
