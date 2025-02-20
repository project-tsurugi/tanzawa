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
package com.tsurugidb.tools.common.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.value.Property;

class CompositeMonitorTest {

    static class Mock implements Monitor {

        int start = 0;

        final List<String> data = new ArrayList<>();

        int success = 0;

        int failure = 0;

        int exception = 0;

        int close = 0;

        @Override
        public void onStart() {
            start++;
        }

        @Override
        public void onData(String format, List<? extends Property> properties) throws MonitoringException {
            data.add(format);
        }

        @Override
        public void onSuccess() {
            success++;
        }

        @Override
        public void onFailure(Throwable cause, DiagnosticCode code, List<?> arguments) throws MonitoringException {
            failure++;
        }

        @Override
        public void onFailure(DiagnosticException occurred) throws MonitoringException {
            exception++;
        }

        @Override
        public void close() {
            close++;
        }
    }

    @Test
    void onStart() throws Exception {
        var m1 = new Mock();
        var m2 = new Mock();
        var m3 = new Mock();
        try (var monitor = new CompositeMonitor(m1, m2, m3)) {
            assertEquals(0, m1.start);
            assertEquals(0, m2.start);
            assertEquals(0, m3.start);

            monitor.onStart();
            assertEquals(1, m1.start);
            assertEquals(1, m2.start);
            assertEquals(1, m3.start);
        }
    }

    @Test
    void onData() throws Exception {
        var m1 = new Mock();
        var m2 = new Mock();
        var m3 = new Mock();
        try (var monitor = new CompositeMonitor(m1, m2, m3)) {
            assertEquals(List.of(), m1.data);
            assertEquals(List.of(), m2.data);
            assertEquals(List.of(), m3.data);

            monitor.onData("testing");
            assertEquals(List.of("testing"), m1.data);
            assertEquals(List.of("testing"), m2.data);
            assertEquals(List.of("testing"), m3.data);
        }
    }

    @Test
    void onSuccess() throws Exception {
        var m1 = new Mock();
        var m2 = new Mock();
        var m3 = new Mock();
        try (var monitor = new CompositeMonitor(m1, m2, m3)) {
            assertEquals(0, m1.success);
            assertEquals(0, m2.success);
            assertEquals(0, m3.success);

            monitor.onSuccess();
            assertEquals(1, m1.success);
            assertEquals(1, m2.success);
            assertEquals(1, m3.success);
        }
    }

    @Test
    void onFailure() throws Exception {
        var m1 = new Mock();
        var m2 = new Mock();
        var m3 = new Mock();
        try (var monitor = new CompositeMonitor(m1, m2, m3)) {
            assertEquals(0, m1.failure);
            assertEquals(0, m2.failure);
            assertEquals(0, m3.failure);

            monitor.onFailure(MonitoringDiagnosticCode.OUTPUT_ERROR, List.of("TESTING"));
            assertEquals(1, m1.failure);
            assertEquals(1, m2.failure);
            assertEquals(1, m3.failure);
        }
    }

    @Test
    void onFailure_exception() throws Exception {
        var m1 = new Mock();
        var m2 = new Mock();
        var m3 = new Mock();
        try (var monitor = new CompositeMonitor(m1, m2, m3)) {
            assertEquals(0, m1.exception);
            assertEquals(0, m2.exception);
            assertEquals(0, m3.exception);

            monitor.onFailure(new MonitoringException(MonitoringDiagnosticCode.OUTPUT_ERROR, List.of("testing")));
            assertEquals(1, m1.exception);
            assertEquals(1, m2.exception);
            assertEquals(1, m3.exception);
        }
    }

    @Test
    void close() throws Exception {
        var m1 = new Mock();
        var m2 = new Mock();
        var m3 = new Mock();
        try (var monitor = new CompositeMonitor(m1, m2, m3)) {
            assertEquals(0, m1.close);
            assertEquals(0, m2.close);
            assertEquals(0, m3.close);

            monitor.close();
            assertEquals(1, m1.close);
            assertEquals(1, m2.close);
            assertEquals(1, m3.close);
        }
    }
}
