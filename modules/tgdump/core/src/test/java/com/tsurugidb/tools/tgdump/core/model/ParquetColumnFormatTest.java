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
