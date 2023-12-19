package com.tsurugidb.tools.tgdump.core.model;

import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * Represents a table dump target.
 */
public class DumpTarget {

    private final String tableName;

    private final Path destination;

    /**
     * Creates a new instance.
     * @param tableName the source table name
     * @param destination the dump destination path (directory)
     */
    public DumpTarget(@Nonnull String tableName, Path destination) {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(destination);
        this.tableName = tableName;
        this.destination = destination;
    }

    /**
     * Returns the source table name.
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Returns the destination path.
     * @return the destination path
     */
    public Path getDestination() {
        return destination;
    }

    @Override
    public int hashCode() {
        return Objects.hash(destination, tableName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DumpTarget other = (DumpTarget) obj;
        return Objects.equals(destination, other.destination) && Objects.equals(tableName, other.tableName);
    }

    @Override
    public String toString() {
        return String.format("DumpTarget [tableName=%s, destination=%s]", tableName, destination); //$NON-NLS-1$
    }
}
