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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.common.monitoring.MonitoringException;
import com.tsurugidb.tsubakuro.sql.TableMetadata;

/**
 * A {@link DumpMonitor} that dispatches each message to sub-monitors.
 */
public class CompositeDumpMonitor implements DumpMonitor {

    private final DumpMonitor[] elements;

    /**
     * Creates a new instance.
     * @param elements the element monitors
     */
    public CompositeDumpMonitor(@Nonnull DumpMonitor... elements) {
        Objects.requireNonNull(elements);
        this.elements = Arrays.copyOf(elements, elements.length);
    }

    /**
     * Creates a new instance.
     * @param elements the element monitors
     */
    public CompositeDumpMonitor(@Nonnull List<? extends DumpMonitor> elements) {
        Objects.requireNonNull(elements);
        this.elements = elements.toArray(new DumpMonitor[elements.size()]);
    }

    @Override
    public void verbose(@Nonnull String format, @Nonnull Object... arguments) throws MonitoringException {
        Objects.requireNonNull(format);
        Objects.requireNonNull(arguments);
        for (var element : elements) {
            element.verbose(format, arguments);
        }
    }

    @Override
    public void onDumpInfo(@Nonnull String tableName, @Nonnull TableMetadata tableInfo, @Nonnull Path dumpDirectory)
            throws MonitoringException {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(tableInfo);
        Objects.requireNonNull(dumpDirectory);
        for (var element : elements) {
            element.onDumpInfo(tableName, tableInfo, dumpDirectory);
        }
    }

    @Override
    public void onDumpInfo(String label, String query, Path dumpDirectory) throws MonitoringException {
        Objects.requireNonNull(label);
        Objects.requireNonNull(query);
        Objects.requireNonNull(dumpDirectory);
        for (var element : elements) {
            element.onDumpInfo(label, query, dumpDirectory);
        }
    }

    @Override
    public void onDumpStart(@Nonnull String tableName, @Nonnull Path dumpDirectory) throws MonitoringException {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(dumpDirectory);
        for (var element : elements) {
            element.onDumpStart(tableName, dumpDirectory);
        }
    }

    @Override
    public void onDumpFile(@Nonnull String tableName, @Nonnull Path dumpFile) throws MonitoringException {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(dumpFile);
        for (var element : elements) {
            element.onDumpFile(tableName, dumpFile);
        }
    }

    @Override
    public void onDumpFinish(@Nonnull String tableName, @Nonnull Path dumpDirectory) throws MonitoringException {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(dumpDirectory);
        for (var element : elements) {
            element.onDumpFinish(tableName, dumpDirectory);
        }
    }
}
