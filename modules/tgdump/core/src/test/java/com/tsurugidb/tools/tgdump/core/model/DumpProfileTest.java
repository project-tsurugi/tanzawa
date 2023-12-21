package com.tsurugidb.tools.tgdump.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.tsurugidb.sql.proto.SqlRequest;

class DumpProfileTest {

    @Test
    void defaults() {
        var p = new DumpProfile();
        assertEquals(Optional.empty(), p.getTitle());
        assertEquals(Optional.empty(), p.getDescription());
        assertEquals(Optional.empty(), p.getLocalizedDescription(Locale.ENGLISH));
        assertEquals(Optional.empty(), p.getFileFormat());

        assertEquals(p, DumpProfile.newBuilder().build(), p.toString());
    }

    @Test
    void title() {
        var p = DumpProfile.newBuilder()
                .withTitle("testing")
                .build();
        assertEquals(Optional.of("testing"), p.getTitle());
    }

    @Test
    void description() {
        var p = DumpProfile.newBuilder()
                .withDescription("testing")
                .build();
        assertEquals(Optional.of("testing"), p.getDescription());
    }

    @Test
    void localized_description() {
        var p = DumpProfile.newBuilder()
                .withLocalizedDescription("en", "English")
                .withLocalizedDescription("ja", "Japanese")
                .build();
        assertEquals(Optional.of("English"), p.getLocalizedDescription(Locale.ENGLISH));
        assertEquals(Optional.of("Japanese"), p.getLocalizedDescription(Locale.JAPANESE));
        assertEquals(Optional.empty(), p.getLocalizedDescription(Locale.CHINESE));
    }

    @Test
    void file_foramat() {
        var p = DumpProfile.newBuilder()
                .withFileFormat(new ParquetFileFormat())
                .build();
        assertEquals(Optional.of(new ParquetFileFormat()), p.getFileFormat());
    }

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
