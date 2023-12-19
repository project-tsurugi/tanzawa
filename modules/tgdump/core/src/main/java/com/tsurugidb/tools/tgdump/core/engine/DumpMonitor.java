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
     * Invoked when a table dump operation was started.
     * @param tableName the target table name
     * @param dumpDirectory the dump destination directory for the table
     * @throws MonitoringException if error was occurred while monitoring the event
     */
    void onDumpStart(@Nonnull String tableName, @Nonnull Path dumpDirectory) throws MonitoringException;

    /**
     * Invoked when a dump file is provided.
     * @param tableName the target table name
     * @param dumpFile the provided file path
     * @throws MonitoringException if error was occurred while monitoring the event
     */
    void onDumpFile(@Nonnull String tableName, @Nonnull Path dumpFile) throws MonitoringException;

    /**
     * Invoked when a table dump operation was finished for the table.
     * @param tableName the target table name
     * @param dumpDirectory the dump destination directory for the table
     * @throws MonitoringException if error was occurred while monitoring the event
     */
    void onDumpFinish(@Nonnull String tableName, @Nonnull Path dumpDirectory) throws MonitoringException;
}