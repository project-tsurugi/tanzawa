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
