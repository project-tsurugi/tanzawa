package com.tsurugidb.tools.tgdump.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tools.tgdump.core.model.TransactionSettings;

class ConsoleDumpMonitorTest {

    final List<String> output = new ArrayList<>();

    final Printer printer = new Printer() {
        @Override
        public void print(String message) {
            assertFalse(message.indexOf('\n') >= 0);
            output.add(message);
        }
    };

    @Test
    void verbose_on() {
        var monitor = new ConsoleDumpMonitor(printer, true);
        monitor.verbose("testing: {0}", "ok");
        assertEquals(List.of("testing: ok"), output);
    }

    @Test
    void verbose_off() {
        var monitor = new ConsoleDumpMonitor(printer, false);
        monitor.verbose("testing: {0}", "ok");
        assertEquals(List.of(), output);
    }

    @Test
    void verbose_protobuf() {
        var monitor = new ConsoleDumpMonitor(printer, true);
        monitor.verbose("testing: {0}", TransactionSettings.newBuilder().build().toProtocolBuffer(List.of("A")));
    }

    @Test
    void onDumpInfo_on() {
        var monitor = new ConsoleDumpMonitor(printer, true);
        monitor.onDumpInfo("XXX", new MockTableMetadata("XXX"), Path.of("x"));
        assertEquals(1, output.size());
        assertTrue(output.get(0).contains("XXX"));
    }

    @Test
    void onDumpInfo_off() {
        var monitor = new ConsoleDumpMonitor(printer, false);
        monitor.onDumpInfo("XXX", new MockTableMetadata("XXX"), Path.of("x"));
        assertEquals(0, output.size());
    }

    @Test
    void onDumpStart() {
        var monitor = new ConsoleDumpMonitor(printer, false);
        monitor.onDumpStart("XXX", Path.of("x"));
        assertEquals(1, output.size());
        assertTrue(output.get(0).contains("XXX"));
    }

    @Test
    void onDumpFile_on() {
        var monitor = new ConsoleDumpMonitor(printer, true);
        monitor.onDumpFile("XXX", Path.of("x"));
        assertEquals(1, output.size());
        assertTrue(output.get(0).contains("XXX"));
    }

    @Test
    void onDumpFile_off() {
        var monitor = new ConsoleDumpMonitor(printer, false);
        monitor.onDumpFile("XXX", Path.of("x"));
        assertEquals(0, output.size());
    }

    @Test
    void onDumpFinish() {
        var monitor = new ConsoleDumpMonitor(printer, false);
        monitor.onDumpFinish("XXX", Path.of("x"));
        assertEquals(1, output.size());
        assertTrue(output.get(0).contains("XXX"));
    }
}
