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
package com.tsurugidb.tgsql.core.executor.result;

import static com.tsurugidb.tsubakuro.sql.Types.column;
import static com.tsurugidb.tsubakuro.sql.Types.row;
import static com.tsurugidb.tsubakuro.sql.Types.user;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.sql.proto.SqlResponse;
import com.tsurugidb.tgsql.core.executor.IoSupplier;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.ResultSetMetadata;
import com.tsurugidb.tsubakuro.sql.impl.ResultSetMetadataAdapter;
import com.tsurugidb.tsubakuro.sql.impl.testing.Relation;
import com.tsurugidb.tsubakuro.sql.io.DateTimeInterval;

class BasicResultProcessorTest {

    static final Logger LOG = LoggerFactory.getLogger(BasicResultProcessorTest.class);

    private final IoSupplier<Writer> sink = new IoSupplier<>() {

        @Override
        public Writer get() throws IOException {
            return new StringWriter() {
                private boolean closed = false;
                @Override
                public void close() throws IOException {
                    if (closed) {
                        return;
                    }
                    outputs.add(toString());
                    closed = true;
                }
            };
        }
    };

    private final List<String> outputs = new ArrayList<>();

    @AfterEach
    void dump() {
        for (String s : outputs) {
            for (String line : s.split(Pattern.quote(System.lineSeparator()))) {
                LOG.debug(line);
            }
        }
    }

    @Test
    void simple() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { 1 },
        }).getResultSet(meta(column("a", int.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void matrix() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { 1, "A", new BigDecimal("100") },
            { 2, "B", new BigDecimal("200") },
            { 3, "C", new BigDecimal("300") },
        }).getResultSet(meta(column(int.class), column(String.class), column(BigDecimal.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void header_array() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {})
                .getResultSet(meta(column("a", int[].class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void header_row() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {})
                .getResultSet(meta(column("a", row(column(int.class), column(String.class), column(BigDecimal.class)))));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void header_user() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {})
                .getResultSet(meta(column("a", user("U"))));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_null() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { null },
        }).getResultSet(meta(column(int.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_boolean() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { true },
        }).getResultSet(meta(column(boolean.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_int4() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { 125 },
        }).getResultSet(meta(column(long.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_int8() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { 1L },
        }).getResultSet(meta(column(long.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_float4() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { 1.f },
        }).getResultSet(meta(column(float.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_float8() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { 1.d },
        }).getResultSet(meta(column(double.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_decimal() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { new BigDecimal("3.14") },
        }).getResultSet(meta(column(BigDecimal.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_character() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { "Hello, world!" },
        }).getResultSet(meta(column(String.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_octet() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { new byte[] { 1, 2, 3 } },
        }).getResultSet(meta(column(byte[].class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_bit() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { new boolean[] { true, false } },
        }).getResultSet(meta(column(boolean[].class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_date() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { LocalDate.of(2000, 1, 1) },
        }).getResultSet(meta(column(LocalDate.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_time_point() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { LocalDateTime.of(2022, 9, 22, 12, 28) },
        }).getResultSet(meta(column(LocalDateTime.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_time_point_with_time_zone() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { OffsetDateTime.of(2022, 9, 22, 12, 28, 59, 0, ZoneOffset.ofHours(9)) },
        }).getResultSet(meta(column(OffsetDateTime.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_time_of_day() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { LocalTime.of(1, 2, 3) },
        }).getResultSet(meta(column(LocalTime.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_time_of_day_with_time_zone() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { OffsetTime.of(1, 2, 3, 4, ZoneOffset.ofHours(9)) },
        }).getResultSet(meta(column(OffsetTime.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_date_time_interval() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { new DateTimeInterval(1, 2, 3, 4L) },
        }).getResultSet(meta(column(DateTimeInterval.class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_array() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            {  Relation.array(1, 2, 3) },
        }).getResultSet(meta(column(int[].class)));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    @Test
    void value_row() throws Exception {
        ResultSet rs = Relation.of(new Object[][] {
            { Relation.row(1, "OK", new BigDecimal("3.14")) },
        }).getResultSet(meta(column(row(column(int.class), column(String.class), column(BigDecimal.class)))));

        try (var proc = create()) {
            proc.process(rs);
        }
        assertEquals(1, outputs.size());
    }

    private BasicResultProcessor create() {
        return new BasicResultProcessor(sink, new JsonFactory());
    }

    private static ResultSetMetadata meta(SqlCommon.Column... columns) {
        return new ResultSetMetadataAdapter(SqlResponse.ResultSetMetadata.newBuilder()
                .addAllColumns(Arrays.asList(columns))
                .build());
    }
}
