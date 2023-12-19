package com.tsurugidb.tools.tgdump.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.tsurugidb.sql.proto.SqlRequest;

class TransactionSettingsTest {

    @Test
    void defaults() {
        var s = new TransactionSettings();
        assertEquals(TransactionSettings.DEFAULT_TYPE, s.getType());
        assertEquals(Optional.empty(), s.getLabel());
        assertEquals(TransactionSettings.DEFAULT_ENABLE_READ_AREAS, s.isEnableReadAreas());
    }

    @Test
    void toProtocolBuffer_occ() {
        var s = TransactionSettings.newBuilder()
                .withType(TransactionSettings.Type.OCC)
                .withLabel("TESTING")
                .build();

        assertEquals(
                SqlRequest.TransactionOption.newBuilder()
                        .setType(SqlRequest.TransactionType.SHORT)
                        .setLabel("TESTING")
                        .build(),
                s.toProtocolBuffer(List.of("T1")));
    }

    @Test
    void toProtocolBuffer_rtx() {
        var s = TransactionSettings.newBuilder()
                .withType(TransactionSettings.Type.RTX)
                .withLabel("TESTING")
                .build();

        assertEquals(
                SqlRequest.TransactionOption.newBuilder()
                        .setType(SqlRequest.TransactionType.READ_ONLY)
                        .setLabel("TESTING")
                        .build(),
                s.toProtocolBuffer(List.of("T1")));
    }

    @Test
    void toProtocolBuffer_ltx() {
        var s = TransactionSettings.newBuilder()
                .withType(TransactionSettings.Type.LTX)
                .withLabel("TESTING")
                .withEnableReadAreas(false)
                .build();

        assertEquals(
                SqlRequest.TransactionOption.newBuilder()
                        .setType(SqlRequest.TransactionType.LONG)
                        .setLabel("TESTING")
                        .build(),
                s.toProtocolBuffer(List.of("T1")));
    }

    @Test
    void toProtocolBuffer_ltx_read_area() {
        var s = TransactionSettings.newBuilder()
                .withType(TransactionSettings.Type.LTX)
                .withLabel("TESTING")
                .withEnableReadAreas(true)
                .build();

        assertEquals(
                SqlRequest.TransactionOption.newBuilder()
                        .setType(SqlRequest.TransactionType.LONG)
                        .addInclusiveReadAreas(SqlRequest.ReadArea.newBuilder().setTableName("T1"))
                        .setLabel("TESTING")
                        .build(),
                s.toProtocolBuffer(List.of("T1")));
    }
}
