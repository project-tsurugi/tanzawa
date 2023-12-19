package com.tsurugidb.tools.tgdump.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tsurugidb.sql.proto.SqlRequest;

class DumpProfileTest {

    @Test
    void toProtocolBuffer_default() {
        var p = new DumpProfile();
        var result = p.toProtocolBuffer();
        assertEquals(SqlRequest.DumpOption.getDefaultInstance(), result);
        assertEquals(SqlRequest.DumpOption.FileFormatCase.FILEFORMAT_NOT_SET, result.getFileFormatCase());
    }

    @Test
    void toProtocolBuffer_parquet() {
        var profile = DumpProfile.newBuilder()
                .withFileFormat(new ParquetFileFormat())
                .build();

        var pb = SqlRequest.DumpOption.newBuilder();
        pb.getParquetBuilder()
                .build();
        var expect = pb.build();

        var result = profile.toProtocolBuffer();
        assertEquals(expect, result);
        assertEquals(SqlRequest.DumpOption.FileFormatCase.PARQUET, result.getFileFormatCase());
    }

    @Test
    void toProtocolBuffer_arrow() {
        var profile = DumpProfile.newBuilder()
                .withFileFormat(new ArrowFileFormat())
                .build();

        var pb = SqlRequest.DumpOption.newBuilder();
        pb.getArrowBuilder()
                .build();
        var expect = pb.build();

        var result = profile.toProtocolBuffer();
        assertEquals(expect, result);
        assertEquals(SqlRequest.DumpOption.FileFormatCase.ARROW, result.getFileFormatCase());
    }
}
