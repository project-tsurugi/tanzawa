package com.tsurugidb.tools.tgdump.cli;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.tsurugidb.tools.tgdump.core.engine.DumpMonitor;
import com.tsurugidb.tsubakuro.sql.TableMetadata;

/**
 * An implementation of {@link DumpMonitor} that just the print verbose messages.
 */
public class ConsoleDumpMonitor implements DumpMonitor {

    private final Printer output;

    private final boolean verbose;

    /**
     * Creates a new instance.
     * @param output the message output
     * @param verbose whether or not to print verbose messages
     */
    public ConsoleDumpMonitor(@Nonnull Printer output, boolean verbose) {
        Objects.requireNonNull(output);
        this.output = output;
        this.verbose = verbose;
    }

    private void print(String format, Object... arguments) {
        Object[] args = arguments;
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i] instanceof Message) {
                // escape before edit
                if (args == arguments) {
                    args = Arrays.copyOf(arguments, arguments.length);
                }
                args[i] = TextFormat.shortDebugString((Message) arguments[i]);
            }
        }
        output.print(MessageFormat.format(format, args)); //$NON-NLS-1$
    }

    @Override
    public void verbose(@Nonnull String format, @Nonnull Object... arguments) {
        Objects.requireNonNull(format);
        Objects.requireNonNull(arguments);
        if (verbose) {
            print(format, arguments);
        }
    }

    @Override
    public void onDumpInfo(@Nonnull String tableName, @Nonnull TableMetadata tableInfo, @Nonnull Path dumpDirectory) {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(tableInfo);
        Objects.requireNonNull(dumpDirectory);
        if (verbose) {
            var metadata = new StringBuilder();
            metadata.append('{');
            tableInfo.getDatabaseName().ifPresent(
                    it -> metadata.append("database_name=").append(it).append(", ")); //$NON-NLS-1$; //$NON-NLS-2$
            tableInfo.getSchemaName().ifPresent(
                    it -> metadata.append("schema_name=").append(it).append(", ")); //$NON-NLS-1$; //$NON-NLS-2$
            metadata.append("table_name=").append(tableInfo.getTableName()).append(", "); //$NON-NLS-1$ //$NON-NLS-2$
            metadata.append("columns=[").append(tableInfo.getColumns().stream() //$NON-NLS-1$
                    .map(TextFormat::shortDebugString)
                    .collect(Collectors.joining(", "))).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
            metadata.append('}');
            verbose("found table: table={0}, metadata={1}", tableName, metadata.toString());
        }
    }

    @Override
    public void onDumpStart(@Nonnull String tableName, @Nonnull Path dumpDirectory) {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(dumpDirectory);
        print("table dump operation was started: table={0}, output={1}", tableName, dumpDirectory);
    }

    @Override
    public void onDumpFile(@Nonnull String tableName, @Nonnull Path dumpFile) {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(dumpFile);
        verbose("generated a part of table dump file: table={0}, output={1}", tableName, dumpFile);
    }

    @Override
    public void onDumpFinish(@Nonnull String tableName, @Nonnull Path dumpDirectory) {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(dumpDirectory);
        print("table dump operation was finished: table={0}, output={1}", tableName, dumpDirectory);
    }

    @Override
    public String toString() {
        return "DumpMonitor(console)"; //$NON-NLS-1$
    }
}
