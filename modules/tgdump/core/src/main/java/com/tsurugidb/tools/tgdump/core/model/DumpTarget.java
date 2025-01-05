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
package com.tsurugidb.tools.tgdump.core.model;

import java.nio.file.Path;
import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * Represents target of dump operation.
 */
public class DumpTarget {

    /**
     * Represents a target type.
     */
    public enum TargetType {

        /**
         * Operation was represented as a query text.
         */
        QUERY,

        /**
         * Represents a table target.
         */
        TABLE,
    }

    private final TargetType targetType;

    private final String label;

    private final String target;

    private final Path destination;

    /**
     * Creates a new instance.
     * @param targetType the target type
     * @param label the target label
     * @param target the target text depending on the target type
     * @param destination the dump destination path (directory)
     */
    public DumpTarget(
            @Nonnull TargetType targetType,
            @Nonnull String label,
            @Nonnull String target,
            @Nonnull Path destination) {
        Objects.requireNonNull(targetType);
        Objects.requireNonNull(label);
        Objects.requireNonNull(target);
        Objects.requireNonNull(destination);
        this.targetType = targetType;
        this.label = label;
        this.target = target;
        this.destination = destination;
    }

    /**
     * Creates a new instance for a dump target.
     * @param tableName the source table name
     * @param destination the dump destination path (directory)
     */
    public DumpTarget(@Nonnull String tableName, Path destination) {
        this(TargetType.TABLE, tableName, tableName, destination);
    }

    /**
     * Returns the target type.
     * @return the target type
     */
    public TargetType getTargetType() {
        return targetType;
    }

    /**
     * Returns the target text for the database dump operation.
     * This can either be the name of a table or a SQL text, depending on {@link #getTargetType()}.
     * @return the target text
     * @see #getTargetType()
     */
    public String getTarget() {
        return target;
    }

    /**
     * Returns the target label.
     * @return the target label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the source table name.
     * @return the table name
     * @throws IllegalStateException if the {@link #getTargetType() target type} is not {@link TargetType#TABLE}
     */
    public String getTableName() {
        if (targetType != TargetType.TABLE) {
            throw new IllegalStateException("Target type must be TABLE");
        }
        return target;
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
        return Objects.hash(targetType, label, target, destination);
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
        return targetType == other.targetType 
                && Objects.equals(label, other.label)
                && Objects.equals(target, other.target)
                && Objects.equals(destination, other.destination);
    }

    @Override
    public String toString() {
        return String.format(
                "DumpTarget [targetType=%s, label=%s, target=%s, destination=%s]", //$NON-NLS1$
                targetType, label, target, destination);
    }
}
