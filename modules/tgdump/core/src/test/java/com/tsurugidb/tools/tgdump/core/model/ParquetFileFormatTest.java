package com.tsurugidb.tools.tgdump.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

import com.tsurugidb.sql.proto.SqlRequest;

class ParquetFileFormatTest {

    @Test
    void defaults() {
        var f = new ParquetFileFormat();

        assertEquals(DumpFileFormat.FormatType.PARQUET, f.getFormatType());
        assertEquals(Optional.empty(), f.getParquetVersion());
        assertEquals(OptionalLong.empty(), f.getRecordBatchSize());
        assertEquals(OptionalLong.empty(), f.getRecordBatchInBytes());
        assertEquals(Optional.empty(), f.getCodec());
        assertEquals(Optional.empty(), f.getEncoding());
        assertEquals(List.of(), f.getColumns());

        assertEquals(f, ParquetFileFormat.newBuilder().build(), f.toString());

        assertEquals(SqlRequest.ParquetFileFormat.getDefaultInstance(), f.toProtocolBuffer());
    }

    @Test
    void parquet_version() {
        var f = ParquetFileFormat.newBuilder()
                .withParquetVersion("2.5")
                .build();

        assertEquals(Optional.of("2.5"), f.getParquetVersion());

        assertEquals(
                SqlRequest.ParquetFileFormat.newBuilder()
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void record_batch_size() {
        var f = ParquetFileFormat.newBuilder()
                .withRecordBatchSize(100L)
                .build();

        assertEquals(OptionalLong.of(100), f.getRecordBatchSize());

        assertEquals(
                SqlRequest.ParquetFileFormat.newBuilder()
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void record_batch_in_bytes() {
        var f = ParquetFileFormat.newBuilder()
                .withRecordBatchInBytes(10000L)
                .build();

        assertEquals(OptionalLong.of(10000), f.getRecordBatchInBytes());

        assertEquals(
                SqlRequest.ParquetFileFormat.newBuilder()
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void codec() {
        var f = ParquetFileFormat.newBuilder()
                .withCodec("gzip")
                .build();

        assertEquals(Optional.of("gzip"), f.getCodec());

        assertEquals(
                SqlRequest.ParquetFileFormat.newBuilder()
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void encoding() {
        var f = ParquetFileFormat.newBuilder()
                .withEncoding("plain")
                .build();

        assertEquals(Optional.of("plain"), f.getEncoding());

        assertEquals(
                SqlRequest.ParquetFileFormat.newBuilder()
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void columns() {
        var f = ParquetFileFormat.newBuilder()
                .withColumns(List.of(
                        new ParquetColumnFormat("testing")))
                .build();

        assertEquals(List.of(new ParquetColumnFormat("testing")), f.getColumns());

        assertEquals(
                SqlRequest.ParquetFileFormat.newBuilder()
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void columns_multiple() {
        var f = ParquetFileFormat.newBuilder()
                .withColumns(List.of(
                        new ParquetColumnFormat("a"),
                        new ParquetColumnFormat("b"),
                        new ParquetColumnFormat("c")))
                .build();

        assertEquals(
                List.of(
                        new ParquetColumnFormat("a"),
                        new ParquetColumnFormat("b"),
                        new ParquetColumnFormat("c")),
                f.getColumns());

        assertEquals(
                SqlRequest.ParquetFileFormat.newBuilder()
                    .build(),
                f.toProtocolBuffer());
    }

    @Test
    void record_batch_size_zero() {
        var b = ParquetFileFormat.newBuilder();
        assertThrows(IllegalArgumentException.class, () -> b.withRecordBatchSize(0L));
    }

    @Test
    void record_batch_in_bytes_zero() {
        var b = ParquetFileFormat.newBuilder();
        assertThrows(IllegalArgumentException.class, () -> b.withRecordBatchInBytes(0L));
    }
}
