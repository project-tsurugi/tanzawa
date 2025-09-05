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
package com.tsurugidb.tools.tgdump.core.model;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.sql.proto.SqlRequest;

/**
 * Settings of transactions.
 */
public class TransactionSettings {

    /**
     * The transaction type.
     */
    public enum Type {

        /**
         * OCC (short) transactions.
         */
        OCC,

        /**
         * Long transactions (LTX).
         */
        LTX,

        /**
         * Read-only transactions (RTX).
         */
        RTX,
    }

    /**
     * A builder of {@link TransactionSettings}.
     */
    public static class Builder {

        Type type = DEFAULT_TYPE;

        @Nullable String label;

        boolean enableReadAreas = DEFAULT_ENABLE_READ_AREAS;

        @Nullable Integer scanParallel;

        /**
         * Creates a new instance from this builder settings.
         * @return the created instance
         */
        public TransactionSettings build() {
            return new TransactionSettings(this);
        }

        /**
         * Sets the transaction type.
         * @param value the type to set
         * @return this
         */
        public Builder withType(@Nonnull Type value) {
            Objects.requireNonNull(value);
            this.type = value;
            return this;
        }

        /**
         * Sets the transaction label.
         * @param value the value to set
         * @return this
         */
        public Builder withLabel(String value) {
            this.label = value;
            return this;
        }

        /**
         * Sets whether or not read area setting is enabled.
         * @param value {@code true} if enabled, otherwise {@code false}
         * @return this
         */
        public Builder withEnableReadAreas(boolean value) {
            this.enableReadAreas = value;
            return this;
        }

        /**
         * Sets the scan parallel value.
         * @param value the value to set, or {@code null} to clear the setting
         * @return this
         * @throws IllegalArgumentException if the given value is negative ({@code < 0})
         */
        public Builder withScanParallel(Integer value) {
            if (value != null && value < 0) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "scan parallel must be >= 0: {0}",
                        value));
            }
            this.scanParallel = value;
            return this;
        }
    }

    /**
     * The default transaction type.
     */
    public static final Type DEFAULT_TYPE = Type.RTX;

    /**
     * The default whether or not read-area setting is available (only for LTXs).
     */
    public static final boolean DEFAULT_ENABLE_READ_AREAS = false;

    private final Type type;

    private final String label;

    private final boolean enableReadAreas;

    private final Integer scanParallel;

    /**
     * Creates a new instance with default settings.
     * @see #newBuilder()
     */
    public TransactionSettings() {
        this(new Builder());
    }

    /**
     * Creates a new instance from the builder.
     * @param builder the source builder
     * @see #newBuilder()
     */
    public TransactionSettings(@Nonnull Builder builder) {
        Objects.requireNonNull(builder);
        this.type = builder.type;
        this.label = builder.label;
        this.enableReadAreas = builder.enableReadAreas;
        this.scanParallel = builder.scanParallel;
    }

    /**
     * Creates a new builder object for this class.
     * @return the created builder object
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Returns the transaction type.
     * @return the transaction type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the transaction label.
     * @return the transaction label
     */
    public Optional<String> getLabel() {
        return Optional.ofNullable(label);
    }

    /**
     * Returns whether or not the read area setting is enabled.
     * @return {@code true} if it is enabled, otherwise {@code false}
     */
    public boolean isEnableReadAreas() {
        return enableReadAreas;
    }

    /**
     * Returns the scan parallel value.
     * @return the scan parallel value, or empty if it is not set
     */
    public OptionalInt getScanParallel() {
        if (scanParallel == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(scanParallel);
    }

    /**
     * Builds transaction options from this settings.
     * @param tables the source tables
     * @return the built protocol buffer object
     */
    public SqlRequest.TransactionOption toProtocolBuffer(@Nonnull List<? extends String> tables) {
        Objects.requireNonNull(tables);
        var options = SqlRequest.TransactionOption.newBuilder();
        var txType = getType();
        switch (txType) {
        case OCC:
            options.setType(SqlRequest.TransactionType.SHORT);
            break;
        case LTX:
            options.setType(SqlRequest.TransactionType.LONG);
            if (isEnableReadAreas()) {
                for (var table : tables) {
                    options.addInclusiveReadAreas(SqlRequest.ReadArea.newBuilder().setTableName(table));
                }
            }
            break;
        case RTX:
            options.setType(SqlRequest.TransactionType.READ_ONLY);
            break;
        default:
            throw new IllegalArgumentException(MessageFormat.format(
                    "unknown transaction type: {0}", //$NON-NLS-1$
                    txType));
        }
        getLabel().ifPresent(options::setLabel);
        getScanParallel().ifPresent(options::setScanParallel);
        return options.build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableReadAreas, label, type, scanParallel);
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
        TransactionSettings other = (TransactionSettings) obj;
        return enableReadAreas == other.enableReadAreas
            && Objects.equals(label, other.label)
            && type == other.type
            && Objects.equals(scanParallel, other.scanParallel);
    }

    @Override
    public String toString() {
        return String.format("TransactionSettings(type=%s, label=%s, scanParallel=%s)", type, label, scanParallel);
    }
}
