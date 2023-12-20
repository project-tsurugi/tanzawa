package com.tsurugidb.tools.tgdump.core.model;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.sql.proto.SqlRequest;

/**
 * A {@link DumpFileFormat} for Apache Parquet.
 */
public class ParquetFileFormat implements DumpFileFormat {

    /**
     * A builder of {@link ParquetFileFormat}.
     */
    public static class Builder {

        /**
         * Creates a new instance from this builder settings.
         * @return the created instance
         */
        public ParquetFileFormat build() {
            return new ParquetFileFormat(this);
        }
    }

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

    @Override
    public SqlRequest.ParquetFileFormat toProtocolBuffer() {
        return SqlRequest.ParquetFileFormat.getDefaultInstance();
    }

    @Override
    public int hashCode() {
        return Objects.hash(FormatType.PARQUET);
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
        return true;
    }

    @Override
    public String toString() {
        // FIXME: more fields
        return "ParquetFileFormat()";
    }
}
