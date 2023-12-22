package com.tsurugidb.tools.tgdump.core.model;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.sql.proto.SqlRequest;

/**
 * A {@link DumpFileFormat} for Apache Parquet.
 */
public class ParquetFileFormat implements DumpFileFormat {

    /**
     * A builder of {@link ParquetFileFormat}.
     */
    public static class Builder {

        @Nullable String parquetVersion;

        @Nullable Long recordBatchSize;

        @Nullable Long recordBatchInBytes;

        @Nullable String codec;

        @Nullable String encoding;

        final List<ParquetColumnFormat> columns = new ArrayList<>();

        /**
         * Sets the parquet file format version.
         * @param value the value to set, or {@code null} to clear it
         * @return this
         */
        public Builder withParquetVersion(@Nullable String value) {
            this.parquetVersion = normalize(value);
            return this;
        }

        /**
         * Sets the maximum number of rows in the same row group.
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
         * Sets the approximately maximum row group size in bytes.
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
         * Sets column specific settings.
         * @param values the values to set
         * @return this
         * @see ParquetColumnFormat#forColumn(String)
         */
        public Builder withColumns(@Nonnull List<? extends ParquetColumnFormat> values) {
            Objects.requireNonNull(values);
            this.columns.clear();
            this.columns.addAll(values);
            return this;
        }

        /**
         * Creates a new instance from this builder settings.
         * @return the created instance
         */
        public ParquetFileFormat build() {
            return new ParquetFileFormat(this);
        }

        private static @Nullable String normalize(@Nullable String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return value;
        }
    }

    private final @Nullable String parquetVersion;

    private final @Nullable Long recordBatchSize;

    private final @Nullable Long recordBatchInBytes;

    private final @Nullable String codec;

    private final @Nullable String encoding;

    private final List<ParquetColumnFormat> columns;

    /**
     * Creates a new instance with default settings.
     * @see #newBuilder()
     */
    public ParquetFileFormat() {
        this(new Builder());
    }

    /**
     * Creates a new instance from the builder.
     * @param builder the source builder
     * @see #newBuilder()
     */
    public ParquetFileFormat(@Nonnull Builder builder) {
        Objects.requireNonNull(builder);
        this.parquetVersion = builder.parquetVersion;
        this.recordBatchSize = builder.recordBatchSize;
        this.recordBatchInBytes = builder.recordBatchInBytes;
        this.codec = builder.codec;
        this.encoding = builder.encoding;
        this.columns = List.copyOf(builder.columns);
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
        return FormatType.PARQUET;
    }

    /**
     * Returns the parquet file format version.
     * @return the parquet file format version, or {@code empty} if it is not defined
     */
    public Optional<String> getParquetVersion() {
        return Optional.ofNullable(parquetVersion);
    }

    /**
     * Returns the maximum number of rows in the same row group.
     * @return the maximum number of rows in the same row group, or {@code empty} if it is not defined
     */
    public OptionalLong getRecordBatchSize() {
        return wrap(recordBatchSize);
    }

    /**
     * Returns the approximately maximum row group size in bytes.
     * @return the max row group size from estimated record size, or {@code empty} if it is not defined
     */
    public OptionalLong getRecordBatchInBytes() {
        return wrap(recordBatchInBytes);
    }

    /**
     * Returns the common compression codec name of the individual columns.
     * @return the common compression codec name of the individual columns, or {@code empty} if it is not defined
     */
    public Optional<String> getCodec() {
        return Optional.ofNullable(codec);
    }

    /**
     * Returns the common encoding type of the individual columns.
     * @return the common encoding type of the individual columns, or {@code empty} if it is not defined
     */
    public Optional<String> getEncoding() {
        return Optional.ofNullable(encoding);
    }

    /**
     * Returns individual column settings.
     * @return individual column settings
     */
    public List<ParquetColumnFormat> getColumns() {
        return columns;
    }

    @Override
    public SqlRequest.ParquetFileFormat toProtocolBuffer() {
        var builder = SqlRequest.ParquetFileFormat.newBuilder();
        getParquetVersion().ifPresent(builder::setParquetVersion);
        getRecordBatchSize().ifPresent(builder::setRecordBatchSize);
        getRecordBatchInBytes().ifPresent(builder::setRecordBatchInBytes);
        getCodec().ifPresent(builder::setCodec);
        getEncoding().ifPresent(builder::setEncoding);
        getColumns().stream()
                .map(ParquetColumnFormat::toProtocolBuffer)
                .forEachOrdered(builder::addColumns);
        return builder.build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getFormatType(),
                parquetVersion,
                recordBatchInBytes, recordBatchSize,
                codec, encoding,
                columns);
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
        ParquetFileFormat other = (ParquetFileFormat) obj;
        return Objects.equals(parquetVersion, other.parquetVersion)
                && Objects.equals(recordBatchInBytes, other.recordBatchInBytes)
                && Objects.equals(recordBatchSize, other.recordBatchSize)
                && Objects.equals(codec, other.codec)
                && Objects.equals(encoding, other.encoding)
                && Objects.equals(columns, other.columns);
    }

    @Override
    public String toString() {
        return String.format(
                "ParquetFileFormat(parquetVersion=%s, recordBatchSize=%s, recordBatchInBytes=%s, " //$NON-NLS-1$
                + "codec=%s, encoding=%s, columns=%s)", //$NON-NLS-1$
                parquetVersion, recordBatchSize, recordBatchInBytes,
                codec, encoding, columns);
    }

    private static OptionalLong wrap(@Nullable Long value) {
        if (value == null) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(value);
    }
}
