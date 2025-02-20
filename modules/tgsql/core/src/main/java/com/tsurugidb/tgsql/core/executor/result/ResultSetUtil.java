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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tgsql.core.executor.result.type.BlobWrapper;
import com.tsurugidb.tgsql.core.executor.result.type.ClobWrapper;
import com.tsurugidb.tgsql.core.executor.sql.TransactionWrapper;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.RelationCursor;
import com.tsurugidb.tsubakuro.sql.RelationMetadata;
import com.tsurugidb.tsubakuro.sql.ResultSet;

/**
 * Utilities about {@link ResultSet}.
 */
public final class ResultSetUtil {

    private static final String FIELD_NAME_PREFIX_UNNAMED = "@#";

    /**
     * Fetches the next row from the cursor. Each atom value will be mapped the original Java type, and arrays and row values are mapped into {@code List<Object>}.
     *
     * @param cursor      the input cursor
     * @param metadata    the input metadata
     * @param destination the result destination
     * @return {@code true} if successfully fetched, or {@code false} otherwise
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    public static boolean fetchNextRow(//
            @Nonnull TransactionWrapper transaction, //
            @Nonnull RelationCursor cursor, //
            @Nonnull RelationMetadata metadata, //
            @Nonnull Consumer<Object> destination) throws IOException, ServerException, InterruptedException {
        Objects.requireNonNull(cursor);
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(destination);
        if (!cursor.nextRow()) {
            return false;
        }
        int columnAt = 0;
        for (var columnInfo : metadata.getColumns()) {
            if (!cursor.nextColumn()) {
                throw new IllegalStateException(MessageFormat.format(//
                        "row data is shorter than the metadata: column={0}, at={1}", //
                        columnInfo, //
                        columnAt + 1));
            }
            Object value = fetchCurrentColumn(transaction, cursor, columnInfo);
            destination.accept(value);
            columnAt++;
        }
        return true;
    }

    private static Object fetchCurrentColumn(//
            @Nonnull TransactionWrapper transaction, //
            @Nonnull RelationCursor cursor, //
            @Nonnull SqlCommon.Column columnInfo) throws IOException, ServerException, InterruptedException {
        return fetchCurrentColumn0(transaction, cursor, columnInfo, columnInfo.getDimension());
    }

    private static Object fetchCurrentColumn0(//
            @Nonnull TransactionWrapper transaction, //
            @Nonnull RelationCursor cursor, //
            @Nonnull SqlCommon.Column columnInfo, //
            int dimension) throws IOException, ServerException, InterruptedException {
        if (cursor.isNull()) {
            return null;
        }
        if (dimension > 0) {
            int count = cursor.beginArrayValue();
            var array = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                if (!cursor.nextColumn()) {
                    throw new IllegalStateException(MessageFormat.format(//
                            "array data is broken: column={0}", //
                            columnInfo));
                }
                Object element = fetchCurrentColumn0(transaction, cursor, columnInfo, dimension - 1);
                array.add(element);
            }
            if (cursor.nextColumn()) {
                throw new IllegalStateException(MessageFormat.format(//
                        "array data is broken: column={0}", //
                        columnInfo));
            }
            return array;
        }
        switch (columnInfo.getTypeInfoCase()) {
        case ATOM_TYPE:
            return fetchCurrentColumnAtom(transaction, cursor, columnInfo, columnInfo.getAtomType());
        case ROW_TYPE:
            return fetchCurrentColumnRow(transaction, cursor, columnInfo, columnInfo.getRowType());
        case USER_TYPE:
        default:
            throw new UnsupportedOperationException(MessageFormat.format(//
                    "unsupported column type: {0}, column={1}", //
                    columnInfo.getTypeInfoCase(), //
                    columnInfo));
        }
    }

    private static Object fetchCurrentColumnAtom(//
            @Nonnull TransactionWrapper transaction, //
            @Nonnull RelationCursor cursor, //
            @Nonnull SqlCommon.Column columnInfo, //
            @Nonnull SqlCommon.AtomType type) throws IOException, ServerException, InterruptedException {
        switch (type) {
        case BIT:
            return cursor.fetchBitValue();
        case BOOLEAN:
            return cursor.fetchBooleanValue();
        case CHARACTER:
            return cursor.fetchCharacterValue();
        case DATE:
            return cursor.fetchDateValue();
        case DATETIME_INTERVAL:
            return cursor.fetchDateTimeIntervalValue();
        case DECIMAL:
            return cursor.fetchDecimalValue();
        case FLOAT4:
            return cursor.fetchFloat4Value();
        case FLOAT8:
            return cursor.fetchFloat8Value();
        case INT4:
            return cursor.fetchInt4Value();
        case INT8:
            return cursor.fetchInt8Value();
        case OCTET:
            return cursor.fetchOctetValue();
        case TIME_OF_DAY:
            return cursor.fetchTimeOfDayValue();
        case TIME_OF_DAY_WITH_TIME_ZONE:
            return cursor.fetchTimeOfDayWithTimeZoneValue();
        case TIME_POINT:
            return cursor.fetchTimePointValue();
        case TIME_POINT_WITH_TIME_ZONE:
            return cursor.fetchTimePointWithTimeZoneValue();
        case BLOB:
            var blob = BlobWrapper.of(cursor.fetchBlob());
            transaction.addObject(BlobWrapper.KEY_NAME, blob);
            return blob;
        case CLOB:
            var clob = ClobWrapper.of(cursor.fetchClob());
            transaction.addObject(ClobWrapper.KEY_NAME, clob);
            return clob;

        case UNKNOWN:
            return null;

        case UNRECOGNIZED:
        case TYPE_UNSPECIFIED:
        default:
            throw new UnsupportedOperationException(MessageFormat.format(//
                    "unsupported column type: {0}, column={1}", //
                    type, //
                    columnInfo));
        }
    }

    private static Object fetchCurrentColumnRow(//
            @Nonnull TransactionWrapper transaction, //
            @Nonnull RelationCursor cursor, //
            @Nonnull SqlCommon.Column columnInfo, //
            @Nonnull SqlCommon.RowType type) throws IOException, ServerException, InterruptedException {
        int count = cursor.beginRowValue();
        var array = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            if (!cursor.nextColumn()) {
                throw new IllegalStateException(MessageFormat.format(//
                        "row value data is broken: column={0}", //
                        columnInfo));
            }
            Object element = fetchCurrentColumn(transaction, cursor, type.getColumns(i));
            array.add(element);
        }
        if (cursor.nextColumn()) {
            throw new IllegalStateException(MessageFormat.format(//
                    "row value is broken: column={0}", //
                    columnInfo));
        }
        return array;
    }

    /**
     * get field name.
     *
     * @param column column
     * @param index  column index
     * @return field name
     */
    public static String getFieldName(SqlCommon.Column column, int index) {
        String name = column.getName();
        if (name.isEmpty()) {
            return String.format("%s%d", FIELD_NAME_PREFIX_UNNAMED, index);
        }
        return name;
    }

    private ResultSetUtil() {
        throw new AssertionError();
    }
}
