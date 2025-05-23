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

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tgsql.core.executor.IoSupplier;
import com.tsurugidb.tgsql.core.executor.sql.TransactionWrapper;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.ResultSetMetadata;

/**
 * A basic implementation of {@link ResultProcessor}.
 *
 * This will print the contents into standard output.
 */
public class BasicResultProcessor implements ResultProcessor {

    private final IoSupplier<? extends Writer> outputs;

    private final JsonFactory factory;

    /**
     * Creates a new instance.
     */
    public BasicResultProcessor() {
        this(new StandardWriterSupplier(), //
                new JsonFactory().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false) //
        );
    }

    /**
     * Creates a new instance.
     *
     * @param outputs the output factory
     * @param factory the JSON output settings.
     */
    public BasicResultProcessor(@Nonnull IoSupplier<? extends Writer> outputs, @Nonnull JsonFactory factory) {
        Objects.requireNonNull(outputs);
        Objects.requireNonNull(factory);
        this.outputs = outputs;
        this.factory = factory;
    }

    @Override
    public long process(TransactionWrapper transaction, @Nonnull ResultSet target) throws ServerException, IOException, InterruptedException {
        List<Object> buffer = new ArrayList<>();
        try (//
                var output = outputs.get(); //
                var generator = factory.createGenerator(output); //
        ) {
            generator.setPrettyPrinter(new MinimalPrettyPrinter(System.lineSeparator()));
            dumpMetadata(generator, target.getMetadata());
            while (ResultSetUtil.fetchNextRow(transaction, target, target.getMetadata(), buffer::add)) {
                dumpRow(generator, buffer, target.getMetadata().getColumns());
                buffer.clear();
            }
            generator.writeRaw(System.lineSeparator());
        }
        return System.nanoTime();
    }

    private void dumpMetadata(JsonGenerator generator, ResultSetMetadata metadata) throws IOException {
        generator.writeRaw("// ");
        generator.writeStartObject();
        var columns = metadata.getColumns();
        writeColumnsMetadata(generator, columns);
        generator.writeEndObject();
    }

    private void writeColumnsMetadata(JsonGenerator generator, List<? extends SqlCommon.Column> columns) throws IOException {
        generator.writeFieldName("columns");
        generator.writeStartArray();
        for (int i = 0, n = columns.size(); i < n; i++) {
            dumpColumnMetadata(generator, columns.get(i), i);
        }
        generator.writeEndArray();
    }

    private void dumpColumnMetadata(JsonGenerator generator, SqlCommon.Column column, int index) throws IOException {
        generator.writeStartObject();
        generator.writeFieldName("name");
        if (column.getName().isEmpty()) {
            generator.writeNull();
        } else {
            generator.writeString(column.getName());
        }

        generator.writeFieldName("label");
        generator.writeString(ResultSetUtil.getFieldName(column, index));

        generator.writeFieldName("type");
        switch (column.getTypeInfoCase()) {
        case ATOM_TYPE:
            generator.writeString(column.getAtomType().name());
            break;
        case ROW_TYPE:
            generator.writeStartObject();
            writeColumnsMetadata(generator, column.getRowType().getColumnsList());
            generator.writeEndObject();
            break;
        case USER_TYPE:
            generator.writeString(column.getUserType().getName());
            break;
        case TYPEINFO_NOT_SET:
            generator.writeNull();
            break;
        default:
            break;
        }

        generator.writeFieldName("dimension");
        generator.writeNumber(column.getDimension());

        generator.writeEndObject();
    }

    private void dumpRow(JsonGenerator generator, List<?> elements, List<? extends SqlCommon.Column> columns) throws IOException {
        assert elements.size() == columns.size();
        generator.writeStartObject();
        for (int i = 0, n = elements.size(); i < n; i++) {
            var value = elements.get(i);
            var column = columns.get(i);
            generator.writeFieldName(ResultSetUtil.getFieldName(column, i));
            dumpValue(generator, value, column, column.getDimension());
        }
        generator.writeEndObject();
    }

    private void dumpValue(//
            JsonGenerator generator, //
            Object value, //
            SqlCommon.Column column, //
            int dimension) throws IOException {
        if (value == null) {
            generator.writeNull();
        } else if (value instanceof String) {
            generator.writeString((String) value);
        } else if (value instanceof Boolean) {
            generator.writeBoolean((boolean) value);
        } else if (value instanceof Integer) {
            generator.writeNumber((Integer) value);
        } else if (value instanceof Long) {
            generator.writeNumber((Long) value);
        } else if (value instanceof Float) {
            generator.writeNumber((Float) value);
        } else if (value instanceof Double) {
            generator.writeNumber((Double) value);
        } else if (value instanceof BigDecimal) {
            generator.writeNumber((BigDecimal) value);
        } else if (value instanceof boolean[]) {
            generator.writeStartArray();
            for (var b : (boolean[]) value) {
                generator.writeNumber(b ? 1 : 0);
            }
            generator.writeEndArray();
        } else if (value instanceof byte[]) {
            generator.writeStartArray();
            for (var b : (byte[]) value) {
                generator.writeNumber((int) b);
            }
            generator.writeEndArray();
        } else if (value instanceof List<?> && dimension > 0) {
            generator.writeStartArray();
            for (var e : (List<?>) value) {
                dumpValue(generator, e, column, dimension - 1);
            }
            generator.writeEndArray();
        } else if (value instanceof List<?> && column.getTypeInfoCase() == SqlCommon.Column.TypeInfoCase.ROW_TYPE) {
            dumpRow(generator, (List<?>) value, column.getRowType().getColumnsList());
        } else {
            generator.writeString(String.valueOf(value));
        }
    }
}
