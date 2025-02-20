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
