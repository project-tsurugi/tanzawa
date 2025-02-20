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

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.sql.proto.SqlRequest;

/**
 * A format description of individual columns in {@link ParquetFileFormat}.
 */
public class ParquetColumnFormat {

    /**
     * A builder of {@link ParquetColumnFormat}.
     */
    public static class Builder {

        final String name;

        @Nullable String codec;

        @Nullable String encoding;

        /**
         * Creates a new instance.
         * @param name the target column name
         */
        public Builder(@Nonnull String name) {
            Objects.requireNonNull(name);
            this.name = name;
        }

        /**
         * Sets common compression codec name of the individual columns.
         * @param value the value to set, or {@code null} to clear it
         * @return this
         */
        public Builder withCodec(@Nullable String value) {
            this.codec = normalize(value);
            return this;
        }

        /**
         * Sets common encoding type of the individual columns.
         * @param value the value to set, or {@code null} to clear it
         * @return this
         */
        public Builder withEncoding(@Nullable String value) {
            this.encoding = normalize(value);
            return this;
        }

        /**
         * Creates a new instance from this builder settings.
         * @return the created instance
         */
        public ParquetColumnFormat build() {
            return new ParquetColumnFormat(this);
        }

        private static @Nullable String normalize(@Nullable String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return value;
        }
    }

    private final @Nullable String name;

    private final @Nullable String codec;

    private final @Nullable String encoding;

    /**
     * Creates a new instance with default settings.
     * @param name the target column name
     * @see #newBuilder(String)
     */
    public ParquetColumnFormat(@Nonnull String name) {
        this(new Builder(name));
    }

    /**
     * Creates a new instance from the builder.
     * @param builder the source builder
     * @see #newBuilder(String)
     */
    public ParquetColumnFormat(@Nonnull Builder builder) {
        Objects.requireNonNull(builder);
        this.name = builder.name;
        this.codec = builder.codec;
        this.encoding = builder.encoding;
    }

    /**
     * Creates a new builder object for this class.
     * @param name the target column name
     * @return the created builder object
     */
    public static Builder newBuilder(@Nonnull String name) {
        return new Builder(name);
    }

    /**
     * Creates a new builder object for this class.
     * @param name the target column name
     * @return the created builder object
     */
    public static Builder forColumn(@Nonnull String name) {
        return newBuilder(name);
    }

    /**
     * Returns the target column name.
     * @return the target column name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the compression codec name for this column.
     * @return the compression codec name, or {@code empty} if it is not defined
     */
    public Optional<String> getCodec() {
        return Optional.ofNullable(codec);
    }

    /**
     * Returns the common encoding type for this column.
     * @return the common encoding type, or {@code empty} if it is not defined
     */
    public Optional<String> getEncoding() {
        return Optional.ofNullable(encoding);
    }

    /**
     * Builds dump options from this settings.
     * @return the built protocol buffer object
     */
    public SqlRequest.ParquetColumnFormat toProtocolBuffer() {
        var builder = SqlRequest.ParquetColumnFormat.newBuilder();
        builder.setName(getName());
        getCodec().ifPresent(builder::setCodec);
        getEncoding().ifPresent(builder::setEncoding);
        return builder.build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, codec, encoding);
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
        ParquetColumnFormat other = (ParquetColumnFormat) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(codec, other.codec)
                && Objects.equals(encoding, other.encoding);
    }

    @Override
    public String toString() {
        return String.format("ParquetColumnFormat(name=%s, codec=%s, encoding=%s)", name, codec, encoding); //$NON-NLS-1$
    }
}
