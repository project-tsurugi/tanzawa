package com.tsurugidb.tools.common.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.tsurugidb.tools.common.value.Array;
import com.tsurugidb.tools.common.value.Property;
import com.tsurugidb.tools.common.value.Record;
import com.tsurugidb.tools.common.value.Value;

class JsonMonitorTest {

    static class Collector implements JsonMonitor.Delegate {

        final List<Map<String, Object>> records = new ArrayList<>();

        @Override
        public void write(Record record) {
            records.add(toMap(record));
        }

        @Override
        public void close() {
            return;
        }

        private static Map<String, Object> toMap(Record record) {
            return record.getProperties().stream()
                    // remove code / arguments
                    .filter(it -> !it.getName().equals("code") && !it.getName().equals("arguments"))
                    // remove nulls, empty arrays
                    .filter(it -> !it.getValue().isNull() && !it.getValue().has(Array.of()))
                    // replace timestamp value with -1
                    .map(it -> {
                        if (it.getName().equals("timestamp")) {
                            return Property.of("timestamp", VALUE_DUMMY_TIMESTAMP);
                        }
                        return it;
                    })
                    .collect(Collectors.toMap(Property::getName, Property::getValue));
        }
    }

    static final Value VALUE_DUMMY_TIMESTAMP = Value.of(-1);

    private final Collector collector = new Collector();

    private final JsonMonitor monitor = new JsonMonitor(collector);

    private final List<Path> temporaryFiles = new ArrayList<>();

    private Path newFile() throws IOException {
        var created = Files.createTempFile(JsonMonitorTest.class.getSimpleName(), ".json");
        temporaryFiles.add(created);
        return created;
    }

    @AfterEach
    void teardown() throws Exception {
        for (var file : temporaryFiles) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void onStart() throws Exception {
        monitor.onStart();
        assertEquals(
                List.of(Map.of(
                        "kind", Value.of("start"),
                        "timestamp", VALUE_DUMMY_TIMESTAMP)),
                collector.records);
    }

    @Test
    void onData() throws Exception {
        monitor.onData("testing", List.of(
                Property.of("a", Value.of(1)),
                Property.of("b", Value.of(2))));
        assertEquals(
                List.of(Map.of(
                        "kind", Value.of("data"),
                        "timestamp", VALUE_DUMMY_TIMESTAMP,
                        "format", Value.of("testing"),
                        "a", Value.of(1),
                        "b", Value.of(2))),
                collector.records);
    }

    @Test
    void onSuccess() throws Exception {
        monitor.onSuccess();
        assertEquals(
                List.of(Map.of(
                        "kind", Value.of("finish"),
                        "timestamp", VALUE_DUMMY_TIMESTAMP,
                        "status", Value.of("success"))),
                collector.records);
    }

    @Test
    void onFailure() throws Exception {
        monitor.onFailure(null, MonitoringDiagnosticCode.OUTPUT_ERROR, List.of("TESTING"));
        assertEquals(1, collector.records.size());
        assertEquals(
                List.of(Map.of(
                        "kind", Value.of("finish"),
                        "timestamp", VALUE_DUMMY_TIMESTAMP,
                        "status", Value.of("failure"),
                        "reason", Value.of("monitor_output"),
                        "message", Value.of(MonitoringDiagnosticCode.OUTPUT_ERROR.getMessage(List.of("TESTING"))))),
                collector.records);
    }

    @Test
    void onFailure_causes() throws Exception {
        var c2 = new RuntimeException("c2");
        var c1 = new RuntimeException("c1", c2);
        var c0 = new RuntimeException("c0", c1);

        monitor.onFailure(c0, MonitoringDiagnosticCode.OUTPUT_ERROR, List.of("TESTING"));
        assertEquals(1, collector.records.size());
        assertEquals(
                List.of(Map.of(
                        "kind", Value.of("finish"),
                        "timestamp", VALUE_DUMMY_TIMESTAMP,
                        "status", Value.of("failure"),
                        "reason", Value.of("monitor_output"),
                        "message", Value.of(MonitoringDiagnosticCode.OUTPUT_ERROR.getMessage(List.of("TESTING"))),
                        "cause", Value.of(Array.of(c0.toString(), c1.toString(), c2.toString())))),
                collector.records);
    }

    @Test
    void onFailureDiagnosticException() throws Exception {
        monitor.onFailure(new MonitoringException(MonitoringDiagnosticCode.OUTPUT_ERROR, List.of("TESTING")));
        assertEquals(1, collector.records.size());
        assertEquals(
                List.of(Map.of(
                        "kind", Value.of("finish"),
                        "timestamp", VALUE_DUMMY_TIMESTAMP,
                        "status", Value.of("failure"),
                        "reason", Value.of("monitor_output"),
                        "message", Value.of(MonitoringDiagnosticCode.OUTPUT_ERROR.getMessage(List.of("TESTING"))))),
                collector.records);
    }

    @Test
    void onFailureDiagnosticException_cause() throws Exception {
        var c2 = new RuntimeException("c2");
        var c1 = new RuntimeException("c1", c2);
        var c0 = new RuntimeException("c0", c1);
        monitor.onFailure(new MonitoringException(MonitoringDiagnosticCode.OUTPUT_ERROR, List.of("TESTING"), c0));
        assertEquals(1, collector.records.size());
        assertEquals(
                List.of(Map.of(
                        "kind", Value.of("finish"),
                        "timestamp", VALUE_DUMMY_TIMESTAMP,
                        "status", Value.of("failure"),
                        "reason", Value.of("monitor_output"),
                        "message", Value.of(MonitoringDiagnosticCode.OUTPUT_ERROR.getMessage(List.of("TESTING"))),
                        "cause", Value.of(Array.of(c0.toString(), c1.toString(), c2.toString())))),
                collector.records);
    }

    @Test
    void json_output() throws Exception {
        var file = newFile();
        try (var writer = new JsonMonitor.JsonOutput(new JsonFactory(), file)) {
            writer.write(Record.of(Property.of("test", 1)));
        }
        var lines = Files.readAllLines(file);
        assertEquals(
                List.of("{\"test\":1}"),
                lines);
    }

    @Test
    void json_output_multiple() throws Exception {
        var file = newFile();
        try (var writer = new JsonMonitor.JsonOutput(new JsonFactory(), file)) {
            writer.write(Record.of(Property.of("t1", 1)));
            assertEquals(
                    List.of(
                            "{\"t1\":1}"),
                    Files.readAllLines(file));


            writer.write(Record.of(Property.of("t2", 2)));
            assertEquals(
                    List.of(
                            "{\"t1\":1}",
                            "{\"t2\":2}"),
                    Files.readAllLines(file));

            writer.write(Record.of(Property.of("t3", 3)));
            assertEquals(
                    List.of(
                            "{\"t1\":1}",
                            "{\"t2\":2}",
                            "{\"t3\":3}"),
                    Files.readAllLines(file));
        }
        var lines = Files.readAllLines(file);
        assertEquals(
                List.of(
                        "{\"t1\":1}",
                        "{\"t2\":2}",
                        "{\"t3\":3}"),
                lines);
    }

    @Test
    void json_output_null() throws Exception {
        var file = newFile();
        try (var writer = new JsonMonitor.JsonOutput(new JsonFactory(), file)) {
            writer.write(Record.of(Property.of("test", null)));
        }
        var lines = Files.readAllLines(file);
        assertEquals(
                List.of("{\"test\":null}"),
                lines);
    }

    @Test
    void json_output_boolean() throws Exception {
        var file = newFile();
        try (var writer = new JsonMonitor.JsonOutput(new JsonFactory(), file)) {
            writer.write(Record.of(Property.of("test", false)));
        }
        var lines = Files.readAllLines(file);
        assertEquals(
                List.of("{\"test\":false}"),
                lines);
    }

    @Test
    void json_output_integer() throws Exception {
        var file = newFile();
        try (var writer = new JsonMonitor.JsonOutput(new JsonFactory(), file)) {
            writer.write(Record.of(Property.of("test", 100)));
        }
        var lines = Files.readAllLines(file);
        assertEquals(
                List.of("{\"test\":100}"),
                lines);
    }

    @Test
    void json_output_decimal() throws Exception {
        var file = newFile();
        try (var writer = new JsonMonitor.JsonOutput(new JsonFactory(), file)) {
            writer.write(Record.of(Property.of("test", new BigDecimal("3.14"))));
        }
        var lines = Files.readAllLines(file);
        assertEquals(
                List.of("{\"test\":3.14}"),
                lines);
    }

    @Test
    void json_output_string() throws Exception {
        var file = newFile();
        try (var writer = new JsonMonitor.JsonOutput(new JsonFactory(), file)) {
            writer.write(Record.of(Property.of("test", "TESTING")));
        }
        var lines = Files.readAllLines(file);
        assertEquals(
                List.of("{\"test\":\"TESTING\"}"),
                lines);
    }

    @Test
    void json_output_array() throws Exception {
        var file = newFile();
        try (var writer = new JsonMonitor.JsonOutput(new JsonFactory(), file)) {
            writer.write(Record.of(Property.of("test", Array.of(1, 2, 3))));
        }
        var lines = Files.readAllLines(file);
        assertEquals(
                List.of("{\"test\":[1,2,3]}"),
                lines);
    }

    @Test
    void json_output_record() throws Exception {
        var file = newFile();
        try (var writer = new JsonMonitor.JsonOutput(new JsonFactory(), file)) {
            writer.write(Record.of(Property.of("test", Record.of(Property.of("a", 1), Property.of("b", 2)))));
        }
        var lines = Files.readAllLines(file);
        assertEquals(
                List.of("{\"test\":{\"a\":1,\"b\":2}}"),
                lines);
    }

    @Test
    void json_output_write_after_close() throws Exception {
        var file = newFile();
        try (var writer = new JsonMonitor.JsonOutput(new JsonFactory(), file)) {
            writer.close();
            assertThrows(IllegalStateException.class, () -> writer.write(Record.of(Property.of("test", 1))));
        }
    }

    @Test
    void json_output_missing_parent() throws Exception {
        var dir = Files.createTempDirectory(JsonMonitorTest.class.getSimpleName());
        Files.delete(dir);
        var file = dir.resolve("orphaned.json");

        temporaryFiles.add(file);
        temporaryFiles.add(dir);
        try (var writer = new JsonMonitor.JsonOutput(new JsonFactory(), file)) {
            writer.write(Record.of(Property.of("test", 1)));
        }
        var lines = Files.readAllLines(file);
        assertEquals(
                List.of("{\"test\":1}"),
                lines);
    }
}
