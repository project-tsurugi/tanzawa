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
package com.tsurugidb.tools.tgdump.core.engine;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tools.common.monitoring.Monitor;
import com.tsurugidb.tools.common.monitoring.MonitoringException;
import com.tsurugidb.tools.common.value.Array;
import com.tsurugidb.tools.common.value.Property;
import com.tsurugidb.tools.common.value.Record;
import com.tsurugidb.tools.common.value.Value;
import com.tsurugidb.tsubakuro.sql.TableMetadata;

/**
 * An adapter of {@link Monitor} to track individual dump operations.
 */
public class BasicDumpMonitor implements DumpMonitor {

    /**
     * The monitoring format name that a target table information was provided.
     */
    public static final String FORMAT_DUMP_INFO = "dump-info";

    /**
     * The monitoring format name that a table dump was started.
     */
    public static final String FORMAT_DUMP_START = "dump-start";

    /**
     * The monitoring format name that a dump file was provided.
     */
    public static final String FORMAT_DUMP_FILE = "dump-file";

    /**
     * The monitoring format name that a table dump was finished.
     */
    public static final String FORMAT_DUMP_FINISH = "dump-finish";

    /**
     * The monitoring property of the table name.
     */
    public static final String PROPERTY_TABLE_NAME = "table";

    /**
     * The monitoring property of the dump destination path.
     */
    public static final String PROPERTY_DESTINATION = "destination";

    /**
     * The monitoring property of the target table columns.
     */
    public static final String PROPERTY_COLUMNS = "columns";

    /**
     * The monitoring property of the table column name.
     */
    public static final String PROPERTY_COLUMN_NAME = "name";

    /**
     * The monitoring property of the table column type.
     */
    public static final String PROPERTY_COLUMN_TYPE = "type";

    private final Monitor monitor;

    /**
     * Creates a new instance.
     * @param monitor the delegated monitor
     */
    public BasicDumpMonitor(@Nonnull Monitor monitor) {
        Objects.requireNonNull(monitor);
        this.monitor = monitor;
    }

    @Override
    public void onDumpInfo(@Nonnull String tableName, @Nonnull TableMetadata tableInfo, @Nonnull Path dumpDirectory)
            throws MonitoringException {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(tableInfo);
        Objects.requireNonNull(dumpDirectory);

        var columns = new ArrayList<Record>();
        for (var column : tableInfo.getColumns()) {
            columns.add(Record.of(
                    Property.of(PROPERTY_COLUMN_NAME, Value.fromObject(column.getName())),
                    Property.of(PROPERTY_COLUMN_TYPE, getTypeName(column))));
        }
        monitor.onData(FORMAT_DUMP_INFO, List.of(
                Property.of(PROPERTY_TABLE_NAME, Value.of(tableName)),
                Property.of(PROPERTY_COLUMNS, Value.of(Array.fromList(columns))),
                Property.of(PROPERTY_DESTINATION, Value.of(dumpDirectory.toString()))));
    }

    private static Value getTypeName(SqlCommon.Column column) {
        switch (column.getTypeInfoCase()) {
        case ATOM_TYPE:
            return Value.of(column.getAtomType().toString());
        case USER_TYPE:
            return Value.of(column.getUserType().getName());
        default:
            return Value.ofNull(); // TODO: fix column name to string
        }
    }

    @Override
    public void onDumpStart(@Nonnull String tableName, @Nonnull Path dumpDirectory) throws MonitoringException {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(dumpDirectory);
        monitor.onData(FORMAT_DUMP_START, List.of(
                Property.of(PROPERTY_TABLE_NAME, Value.of(tableName)),
                Property.of(PROPERTY_DESTINATION, Value.of(dumpDirectory.toString()))));
    }

    @Override
    public void onDumpFile(@Nonnull String tableName, @Nonnull Path dumpFile) throws MonitoringException {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(dumpFile);
        monitor.onData(FORMAT_DUMP_FILE, List.of(
                Property.of(PROPERTY_TABLE_NAME, Value.of(tableName)),
                Property.of(PROPERTY_DESTINATION, Value.of(dumpFile.toString()))));
    }

    @Override
    public void onDumpFinish(@Nonnull String tableName, @Nonnull Path dumpDirectory) throws MonitoringException {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(dumpDirectory);
        monitor.onData(FORMAT_DUMP_FINISH, List.of(
                Property.of(PROPERTY_TABLE_NAME, Value.of(tableName)),
                Property.of(PROPERTY_DESTINATION, Value.of(dumpDirectory.toString()))));
    }

    @Override
    public String toString() {
        return String.format("DumpMonitor(%s)", monitor); //$NON-NLS-1$
    }
}
