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

import java.text.MessageFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.sql.proto.SqlRequest;

/**
 * A {@link DumpFileFormat} for Apache Arrow.
 */
public class ArrowFileFormat implements DumpFileFormat {

    /**
     * {@code CHAR} column metadata type.
     */
    public enum CharacterFieldType {

        /**
         * use {@code StringBuilder} for {@code CHAR} columns.
         */
        STRING,

        /**
         * use {@code FixedSizeBinaryBuilder} for {@code CHAR} columns.
         */
        FIXED_SIZE_BINARY,
    }

    /**
     * A builder of {@link ArrowFileFormat}.
     */
    public static class Builder {

        @Nullable String metadataVersion;

        @Nullable Integer alignment;

        @Nullable Long recordBatchSize;

        @Nullable Long recordBatchInBytes;

        @Nullable String codec;

        @Nullable Double minSpaceSaving;

        @Nullable CharacterFieldType characterFieldType;

        /**
         * Sets the metadata format version.
         * @param value the value to set, or {@code null} to clear it
         * @return this
         */
        public Builder withMetadataVersion(@Nullable String value) {
            this.metadataVersion = normalize(value);
            return this;
        }

        /**
         * Sets the byte alignment of each values.
         * @param value the value to set, or {@code null} to clear it
         * @return this
         * @throws IllegalArgumentException if the value is less than {@code 1}
         */
        public Builder withAlignment(@Nullable Integer value) {
            if (value != null && value < 1) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "alignment size must be >= 1 ({0})",
                        value));
            }
            this.alignment = value;
            return this;
        }

        /**
         * Sets the maximum number of records in record batch.
         * @param value the value to set, or {@code null} to clear it
         * @return this
         * @throws IllegalArgumentException if the value is less than {@code 1}
         */
        public Builder withRecordBatchSize(@Nullable Long value) {
            if (value != null && value < 1) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "record batch size must be >= 1 ({0})",
                        value));
            }
            this.recordBatchSize = value;
            return this;
        }

        /**
         * Sets the approximately maximum size of each record batch in bytes.
         * @param value the value to set, or {@code null} to clear it
         * @return this
         * @throws IllegalArgumentException if the value is less than {@code 1}
         */
        public Builder withRecordBatchInBytes(@Nullable Long value) {
            if (value != null && value < 1) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "record batch size must be >= 1 ({0})",
                        value));
            }
            this.recordBatchInBytes = value;
            return this;
        }

        /**
         * Sets the compression codec name.
         * @param value the value to set, or {@code null} to clear it
         * @return this
         */
        public Builder withCodec(@Nullable String value) {
            this.codec = normalize(value);
            return this;
        }

        /**
         * Sets threshold for adopting compressed data.
         * @param value the value to set, or {@code null} to clear it
         * @return this
         */
        public Builder withMinSpaceSaving(@Nullable Double value) {
            if (value != null && (value < 0.0 || value > 1.0)) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "min space saving must be in range [0.0, 1.0] ({0})",
                        value));
            }
            this.minSpaceSaving = value;
            return this;
        }

        /**
         * Sets encoding type for {@code CHAR} columns.
         * @param value the value to set, or {@code null} to clear it
         * @return this
         */
        public Builder withCharacterFieldType(@Nullable CharacterFieldType value) {
            this.characterFieldType = value;
            return this;
        }

        /**
         * Creates a new instance from this builder settings.
         * @return the created instance
         */
        public ArrowFileFormat build() {
            return new ArrowFileFormat(this);
        }

        private static @Nullable String normalize(@Nullable String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return value;
        }
    }

    static final Logger LOG = LoggerFactory.getLogger(ArrowFileFormat.class);

    private final @Nullable String metadataVersion;

    private final @Nullable Integer alignment;

    private final @Nullable Long recordBatchSize;

    private final @Nullable Long recordBatchInBytes;

    private final @Nullable String codec;

    private final @Nullable Double minSpaceSaving;

    private final @Nullable CharacterFieldType characterFieldType;

    /**
     * Creates a new instance with default settings.
     * @see #newBuilder()
     */
    public ArrowFileFormat() {
        this(new Builder());
    }

    /**
     * Creates a new instance from the builder.
     * @param builder the source builder
     * @see #newBuilder()
     */
    public ArrowFileFormat(@Nonnull Builder builder) {
        Objects.requireNonNull(builder);
        this.metadataVersion = builder.metadataVersion;
        this.alignment = builder.alignment;
        this.recordBatchSize = builder.recordBatchSize;
        this.recordBatchInBytes = builder.recordBatchInBytes;
        this.codec = builder.codec;
        this.minSpaceSaving = builder.minSpaceSaving;
        this.characterFieldType = builder.characterFieldType;
    }

    /**
     * Creates a new builder object for this class.
     * @return the created builder object
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public FormatType getFormatType() {
        return FormatType.ARROW;
    }

    /**
     * Returns the metadata format version.
     * @return the metadata format version, or {@code empty} if it is not defined
     */
    public Optional<String> getMetadataVersion() {
        return Optional.ofNullable(metadataVersion);
    }

    /**
     * Returns the byte alignment of each values.
     * @return the byte alignment of each values, or {@code empty} if it is not defined
     */
    public OptionalInt getAlignment() {
        return wrap(alignment);
    }

    /**
     * Returns the maximum number of records in record batch.
     * @return the record batch size, or {@code empty} if it is not defined
     */
    public OptionalLong getRecordBatchSize() {
        return wrap(recordBatchSize);
    }

    /**
     * Returns the approximately maximum size of each record batch in bytes.
     * @return the record batch size in estimated record size, or {@code empty} if it is not defined
     */
    public OptionalLong getRecordBatchInBytes() {
        return wrap(recordBatchInBytes);
    }

    /**
     * Returns the compression codec name.
     * @return the compression codec name, or {@code empty} if it is not defined
     */
    public Optional<String> getCodec() {
        return Optional.ofNullable(codec);
    }

    /**
     * Returns threshold for adopting compressed data.
     * @return threshold for adopting compressed data, or {@code empty} if it is not defined
     */
    public OptionalDouble getMinSpaceSaving() {
        return wrap(minSpaceSaving);
    }

    /**
     * Returns {@code CHAR} column metadata type.
     * @return {@code CHAR} column metadata type, or {@code empty} if it is not defined
     */
    public Optional<CharacterFieldType> getCharacterFieldType() {
        return Optional.ofNullable(characterFieldType);
    }

    @Override
    public SqlRequest.ArrowFileFormat toProtocolBuffer() {
        var builder = SqlRequest.ArrowFileFormat.newBuilder();
        getMetadataVersion().ifPresent(builder::setMetadataVersion);
        getAlignment().ifPresent(builder::setAlignment);
        getRecordBatchSize().ifPresent(builder::setRecordBatchSize);
        getRecordBatchInBytes().ifPresent(builder::setRecordBatchInBytes);
        getCodec().ifPresent(builder::setCodec);
        getMinSpaceSaving().ifPresent(builder::setMinSpaceSaving);
        getCharacterFieldType().map(ArrowFileFormat::convert).ifPresent(builder::setCharacterFieldType);
        return builder.build();
    }

    private static SqlRequest.ArrowCharacterFieldType convert(CharacterFieldType value) {
        switch (value) {
        case STRING:
            return SqlRequest.ArrowCharacterFieldType.STRING;
        case FIXED_SIZE_BINARY:
            return SqlRequest.ArrowCharacterFieldType.FIXED_SIZE_BINARY;
        }
        // may not occur
        LOG.warn("unrecognized arrow character field type: {}", value);
        return SqlRequest.ArrowCharacterFieldType.UNRECOGNIZED;
    }

    @Override
    public int hashCode() {
        return Objects.hash(alignment, characterFieldType, codec, metadataVersion, minSpaceSaving, recordBatchInBytes,
                recordBatchSize);
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
        ArrowFileFormat other = (ArrowFileFormat) obj;
        return Objects.equals(alignment, other.alignment)
                && characterFieldType == other.characterFieldType
                && Objects.equals(codec, other.codec)
                && Objects.equals(metadataVersion, other.metadataVersion)
                && Objects.equals(minSpaceSaving, other.minSpaceSaving)
                && Objects.equals(recordBatchInBytes, other.recordBatchInBytes)
                && Objects.equals(recordBatchSize, other.recordBatchSize);
    }

    @Override
    public String toString() {
        return String.format(
                "ArrowFileFormat(metadataVersion=%s,alignment=%s, recordBatchSize=%s, recordBatchInBytes=%s, " //$NON-NLS-1$
                + "codec=%s, minSpaceSaving=%s, characterFieldType=%s)", //$NON-NLS-1$
                metadataVersion, alignment, recordBatchSize, recordBatchInBytes,
                codec, minSpaceSaving, characterFieldType);
    }

    private static OptionalInt wrap(@Nullable Integer value) {
        if (value == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(value);
    }

    private static OptionalLong wrap(@Nullable Long value) {
        if (value == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(value);
    }

    private static OptionalDouble wrap(@Nullable Double value) {
        if (value == null) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(value);
    }
}
