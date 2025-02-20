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
package com.tsurugidb.tools.tgdump.core.engine;

import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.common.monitoring.MonitoringException;
import com.tsurugidb.tsubakuro.sql.TableMetadata;

/**
 * Dump operation monitor.
 */
public interface DumpMonitor {

    /**
     * Records a verbose log message.
     * @param format the message format
     * @param arguments the message arguments
     * @throws MonitoringException if error was occurred while monitoring the event
     */
    default void verbose(@Nonnull String format, @Nonnull Object... arguments) throws MonitoringException {
        Objects.requireNonNull(format);
        Objects.requireNonNull(arguments);
    }

    /**
     * Invoked when a target dump table information was provided.
     * @param tableName the target table name
     * @param tableInfo the target table information
     * @param dumpDirectory the dump destination directory for the table
     * @throws MonitoringException if error was occurred while monitoring the event
     */
    void onDumpInfo(@Nonnull String tableName, @Nonnull TableMetadata tableInfo, @Nonnull Path dumpDirectory)
            throws MonitoringException;

    /**
     * Invoked when a query text was validated.
     * @param label the query label
     * @param query the query text
     * @param dumpDirectory the dump destination directory for the operation
     * @throws MonitoringException if error was occurred while monitoring the event
     */
    void onDumpInfo(@Nonnull String label, @Nonnull String query, @Nonnull Path dumpDirectory)
            throws MonitoringException;

    /**
     * Invoked when a dump operation was started.
     * @param tableName the table name or label for the operation
     * @param dumpDirectory the dump destination directory for the operation
     * @throws MonitoringException if error was occurred while monitoring the event
     */
    void onDumpStart(@Nonnull String tableName, @Nonnull Path dumpDirectory) throws MonitoringException;

    /**
     * Invoked when a dump file is provided.
     * @param tableName the table name or label for the operation
     * @param dumpFile the provided file path
     * @throws MonitoringException if error was occurred while monitoring the event
     */
    void onDumpFile(@Nonnull String tableName, @Nonnull Path dumpFile) throws MonitoringException;

    /**
     * Invoked when each dump operation was finished.
     * @param tableName the table name or label for the operation
     * @param dumpDirectory the dump destination directory for the operation
     * @throws MonitoringException if error was occurred while monitoring the event
     */
    void onDumpFinish(@Nonnull String tableName, @Nonnull Path dumpDirectory) throws MonitoringException;
}