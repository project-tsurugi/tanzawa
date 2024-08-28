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
