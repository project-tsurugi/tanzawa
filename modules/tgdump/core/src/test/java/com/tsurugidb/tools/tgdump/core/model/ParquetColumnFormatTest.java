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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.tsurugidb.sql.proto.SqlRequest;

class ParquetColumnFormatTest {

    @Test
    void simple() {
        var f = new ParquetColumnFormat("testing");

        assertEquals("testing", f.getName());
        assertEquals(Optional.empty(), f.getCodec());
        assertEquals(Optional.empty(), f.getEncoding());

        assertEquals(f, ParquetColumnFormat.newBuilder("testing").build(), f.toString());

        assertEquals(
                SqlRequest.ParquetColumnFormat.newBuilder()
                    .setName("testing")
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void codec() {
        var f = ParquetColumnFormat.forColumn(("testing"))
                .withCodec("snappy")
                .build();

        assertEquals(Optional.of("snappy"), f.getCodec());

        assertEquals(
                SqlRequest.ParquetColumnFormat.newBuilder()
                    .setName("testing")
                    .setCodec("snappy")
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void encoding() {
        var f = ParquetColumnFormat.forColumn(("testing"))
                .withEncoding("RLE")
                .build();

        assertEquals(Optional.of("RLE"), f.getEncoding());

        assertEquals(
                SqlRequest.ParquetColumnFormat.newBuilder()
                    .setName("testing")
                    .setEncoding("RLE")
                    .build(),
                f.toProtocolBuffer());
    }
}
