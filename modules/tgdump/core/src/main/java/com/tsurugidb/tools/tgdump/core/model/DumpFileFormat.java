package com.tsurugidb.tools.tgdump.core.model;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.tsurugidb.sql.proto.SqlRequest;

/**
 * Represents a dump file format for {@link DumpProfile}.
 */
public interface DumpFileFormat {

    /**
     * Represents the dump file format type.
     */
    enum FormatType {

        /**
         * Apache Parquet format.
         */
        PARQUET(SqlRequest.DumpOption.getDescriptor().findFieldByName("parquet")), //$NON-NLS-1$

        /**
         * Apache Arrow format.
         */
        ARROW(SqlRequest.DumpOption.getDescriptor().findFieldByName("arrow")), //$NON-NLS-1$
        ;

        private final Descriptors.FieldDescriptor fieldDescriptor;

        FormatType(@Nonnull Descriptors.FieldDescriptor fieldDescriptor) {
            Objects.requireNonNull(fieldDescriptor);
            this.fieldDescriptor = fieldDescriptor;
        }

        /**
         * Returns the field descriptor for the format type.
         * @return the field descriptor
         */
        public Descriptors.FieldDescriptor getFieldDescriptor() {
            return fieldDescriptor;
        }
    }

    /**
     * Returns the dump file format type.
     * @return the dump file format type
     */
    FormatType getFormatType();

    /**
     * Builds file format options from this settings.
     * @return the built protocol buffer object
     */
    Message toProtocolBuffer();
}
