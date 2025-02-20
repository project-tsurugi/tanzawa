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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

import com.tsurugidb.sql.proto.SqlRequest;

class ArrowFileFormatTest {

    @Test
    void defaults() {
        var f = ArrowFileFormat.newBuilder()
                .build();

        assertEquals(DumpFileFormat.FormatType.ARROW, f.getFormatType());
        assertEquals(Optional.empty(), f.getMetadataVersion());
        assertEquals(OptionalInt.empty(), f.getAlignment());
        assertEquals(OptionalLong.empty(), f.getRecordBatchSize());
        assertEquals(OptionalLong.empty(), f.getRecordBatchInBytes());
        assertEquals(Optional.empty(), f.getCodec());
        assertEquals(OptionalDouble.empty(), f.getMinSpaceSaving());
        assertEquals(Optional.empty(), f.getCharacterFieldType());

        assertEquals(new ArrowFileFormat(), f, f.toString());

        assertEquals(SqlRequest.ArrowFileFormat.getDefaultInstance(), f.toProtocolBuffer());
    }

    @Test
    void metadata_version() {
        var f = ArrowFileFormat.newBuilder()
                .withMetadataVersion("4")
                .build();

        assertEquals(Optional.of("4"), f.getMetadataVersion());

        assertEquals(
                SqlRequest.ArrowFileFormat.newBuilder()
                    .setMetadataVersion("4")
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void alignment() {
        var f = ArrowFileFormat.newBuilder()
                .withAlignment(16)
                .build();

        assertEquals(OptionalInt.of(16), f.getAlignment());

        assertEquals(
                SqlRequest.ArrowFileFormat.newBuilder()
                    .setAlignment(16)
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void record_batch_size() {
        var f = ArrowFileFormat.newBuilder()
                .withRecordBatchSize(100L)
                .build();

        assertEquals(OptionalLong.of(100), f.getRecordBatchSize());

        assertEquals(
                SqlRequest.ArrowFileFormat.newBuilder()
                    .setRecordBatchSize(100)
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void record_batch_in_bytes() {
        var f = ArrowFileFormat.newBuilder()
                .withRecordBatchInBytes(10000L)
                .build();

        assertEquals(OptionalLong.of(10000L), f.getRecordBatchInBytes());

        assertEquals(
                SqlRequest.ArrowFileFormat.newBuilder()
                    .setRecordBatchInBytes(10000)
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void codec() {
        var f = ArrowFileFormat.newBuilder()
                .withCodec("gzip")
                .build();

        assertEquals(Optional.of("gzip"), f.getCodec());

        assertEquals(
                SqlRequest.ArrowFileFormat.newBuilder()
                    .setCodec("gzip")
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void min_space_saving() {
        var f = ArrowFileFormat.newBuilder()
                .withMinSpaceSaving(0.75)
                .build();

        assertEquals(OptionalDouble.of(0.75), f.getMinSpaceSaving());

        assertEquals(
                SqlRequest.ArrowFileFormat.newBuilder()
                    .setMinSpaceSaving(0.75)
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void character_field_type() {
        var f = ArrowFileFormat.newBuilder()
                .withCharacterFieldType(ArrowFileFormat.CharacterFieldType.FIXED_SIZE_BINARY)
                .build();

        assertEquals(Optional.of(ArrowFileFormat.CharacterFieldType.FIXED_SIZE_BINARY), f.getCharacterFieldType());

        assertEquals(
                SqlRequest.ArrowFileFormat.newBuilder()
                    .setCharacterFieldType(SqlRequest.ArrowCharacterFieldType.FIXED_SIZE_BINARY)
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void character_field_type_another() {
        var f = ArrowFileFormat.newBuilder()
                .withCharacterFieldType(ArrowFileFormat.CharacterFieldType.STRING)
                .build();

        assertEquals(Optional.of(ArrowFileFormat.CharacterFieldType.STRING), f.getCharacterFieldType());

        assertEquals(
                SqlRequest.ArrowFileFormat.newBuilder()
                    .setCharacterFieldType(SqlRequest.ArrowCharacterFieldType.STRING)
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void alignment_under() {
        var b = ArrowFileFormat.newBuilder();
        assertThrows(IllegalArgumentException.class, () -> b.withAlignment(0));
    }

    @Test
    void record_batch_size_under() {
        var b = ArrowFileFormat.newBuilder();
        assertThrows(IllegalArgumentException.class, () -> b.withRecordBatchSize(0L));
    }

    @Test
    void record_batch_in_bytes_under() {
        var b = ArrowFileFormat.newBuilder();
        assertThrows(IllegalArgumentException.class, () -> b.withRecordBatchInBytes(0L));
    }

    @Test
    void min_space_saving_under() {
        var b = ArrowFileFormat.newBuilder();
        assertThrows(IllegalArgumentException.class, () -> b.withMinSpaceSaving(-0.01));
    }

    @Test
    void min_space_saving_over() {
        var b = ArrowFileFormat.newBuilder();
        assertThrows(IllegalArgumentException.class, () -> b.withMinSpaceSaving(1.01));
    }
}
