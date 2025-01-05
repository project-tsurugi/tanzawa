/*
 * Copyright 2023-2024 Project Tsurugi.
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

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.JsonNode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.tgdump.core.engine.DumpMonitor;
import com.tsurugidb.tools.tgdump.core.model.TransactionSettings;

class MainTest {

    private Path temporaryDir;

    Path getTemporaryDir() throws IOException {
        if (temporaryDir == null) {
            temporaryDir = Files.createTempDirectory(MainTest.class.getSimpleName());
        }
        return temporaryDir;
    }

    @AfterEach
    void teardown() throws IOException {
        if (temporaryDir != null) {
            TestUtil.delete(temporaryDir);
        }
    }

    Stream<JsonNode> readKind(Path monitorFile, String kind) throws IOException {
        return TestUtil.readMonitor(monitorFile)
                .filter(it -> getString(it, "kind").equals(Optional.of(kind)));
    }

    Optional<String> readFinishReason(Path monitorFile) throws IOException {
        return readKind(monitorFile, "finish")
                .filter(it -> getString(it, "status").equals(Optional.of("failure")))
                .flatMap(it -> getString(it, "reason").stream())
                .findAny();
    }

    static Optional<String> getString(JsonNode node, String name) {
        return Optional.of(node.get(name))
                .filter(it -> !it.isNull())
                .map(JsonNode::asText)
                .filter(it -> !it.isBlank());
    }

    @Test
    void execute_help() {
        var app = new Main();
        var status = app.execute("--help");
        assertEquals(0, status);
    }

    @Test
    void execute_version() {
        var app = new Main();
        var status = app.execute("--version");
        assertEquals(0, status);
    }

    @Test
    void execute_parameter_error() {
        var app = new Main();
        var status = app.execute();
        assertEquals(Constants.EXIT_STATUS_PARAMETER_ERROR, status);
    }

    @Test
    void execute_success() throws Exception {
        var app = new Main() {
            @Override
            void executeBody(DumpMonitor monitor, CommandArgumentSet args) {
                return;
            }
        };

        var dir = getTemporaryDir();
        var outputDir = dir.resolve("output");
        var monitorFile = dir.resolve("monitor.json");
        var tables = List.of("testing");

        CommandArgumentSet args = new CommandArgumentSet();
        args.setVerbose(true);
        args.setTableNames(tables);
        args.setDestinationPath(outputDir);
        args.setConnectionUri(URI.create("ipc:testing"));
        args.setMonitorOutputPath(monitorFile);

        var status = app.execute(args);
        assertEquals(Constants.EXIT_STATUS_OK, status);

        assertTrue(Files.isRegularFile(monitorFile));
        assertEquals(1, readKind(monitorFile, "start").count());
        assertEquals(1, readKind(monitorFile, "finish")
                .filter(it -> getString(it, "status")
                            .filter(p -> p.equals("success"))
                            .isPresent())
                .count());
    }

    @Test
    void execute_raise_diagnostics() throws Exception {
        var app = new Main() {
            @Override
            void executeBody(DumpMonitor monitor, CommandArgumentSet args) throws DiagnosticException {
                throw new DiagnosticException(CliDiagnosticCode.UNKNOWN, List.of("testing"));
            }
        };

        var dir = getTemporaryDir();
        var outputDir = dir.resolve("output");
        var monitorFile = dir.resolve("monitor.json");
        var tables = List.of("testing");

        CommandArgumentSet args = new CommandArgumentSet();
        args.setVerbose(true);
        args.setTableNames(tables);
        args.setDestinationPath(outputDir);
        args.setConnectionUri(URI.create("ipc:testing"));
        args.setMonitorOutputPath(monitorFile);

        var status = app.execute(args);
        assertEquals(Constants.EXIT_STATUS_OPERATION_ERROR, status);

        assertTrue(Files.isRegularFile(monitorFile));
        assertEquals(1, readKind(monitorFile, "start").count());
        assertEquals(1, readKind(monitorFile, "finish").count());
        assertEquals(Optional.of("unknown"), readFinishReason(monitorFile));
    }

    @Test
    void execute_io() throws Exception {
        var app = new Main() {
            @Override
            void executeBody(DumpMonitor monitor, CommandArgumentSet args) throws IOException {
                throw new IOException();
            }
        };

        var dir = getTemporaryDir();
        var outputDir = dir.resolve("output");
        var monitorFile = dir.resolve("monitor.json");
        var tables = List.of("testing");

        CommandArgumentSet args = new CommandArgumentSet();
        args.setVerbose(true);
        args.setTableNames(tables);
        args.setDestinationPath(outputDir);
        args.setConnectionUri(URI.create("ipc:testing"));
        args.setMonitorOutputPath(monitorFile);

        var status = app.execute(args);
        assertEquals(Constants.EXIT_STATUS_OPERATION_ERROR, status);

        assertTrue(Files.isRegularFile(monitorFile));
        assertEquals(1, readKind(monitorFile, "start").count());
        assertEquals(1, readKind(monitorFile, "finish").count());
        assertEquals(Optional.of("io"), readFinishReason(monitorFile));
    }

    @Test
    void execute_interrupted() throws Exception {
        var app = new Main() {
            @Override
            void executeBody(DumpMonitor monitor, CommandArgumentSet args) throws InterruptedException {
                throw new InterruptedException();
            }
        };

        var dir = getTemporaryDir();
        var outputDir = dir.resolve("output");
        var monitorFile = dir.resolve("monitor.json");
        var tables = List.of("testing");

        CommandArgumentSet args = new CommandArgumentSet();
        args.setVerbose(true);
        args.setTableNames(tables);
        args.setDestinationPath(outputDir);
        args.setConnectionUri(URI.create("ipc:testing"));
        args.setMonitorOutputPath(monitorFile);

        var status = app.execute(args);
        assertEquals(Constants.EXIT_STATUS_INTERRUPTED, status);

        assertTrue(Files.isRegularFile(monitorFile));
        assertEquals(1, readKind(monitorFile, "start").count());
        assertEquals(1, readKind(monitorFile, "finish").count());
        assertEquals(Optional.of("interrupted"), readFinishReason(monitorFile));
    }

    @Test
    void execute_internal() throws Exception {
        var app = new Main() {
            @Override
            void executeBody(DumpMonitor monitor, CommandArgumentSet args) {
                throw new RuntimeException();
            }
        };

        var dir = getTemporaryDir();
        var outputDir = dir.resolve("output");
        var monitorFile = dir.resolve("monitor.json");
        var tables = List.of("testing");

        CommandArgumentSet args = new CommandArgumentSet();
        args.setVerbose(true);
        args.setTableNames(tables);
        args.setDestinationPath(outputDir);
        args.setConnectionUri(URI.create("ipc:testing"));
        args.setMonitorOutputPath(monitorFile);

        var status = app.execute(args);
        assertEquals(Constants.EXIT_STATUS_INTERNAL_ERROR, status);

        assertTrue(Files.isRegularFile(monitorFile));
        assertEquals(1, readKind(monitorFile, "start").count());
        assertEquals(1, readKind(monitorFile, "finish").count());
        assertEquals(Optional.of("internal"), readFinishReason(monitorFile));
    }

    @Test
    void parseArguments_simple() {
        var app = new Main();
        var args = app.parseArguments("--connection", "ipc:testing", "A", "--to", "output");
        assertEquals(List.of("A"), args.getTableNames());
        assertEquals(Path.of("output"), args.getDestinationPath());
        assertEquals(URI.create("ipc:testing"), args.getConnectionUri());

        // defaults
        assertFalse(args.isQueryMode());
        assertFalse(args.isSingleMode());
        assertEquals(Path.of("default"), args.getProfile());
        assertNull(args.getConnectionLabel());
        assertEquals(0, args.getConnectionTimeoutMillis());
        assertEquals(TransactionSettings.Type.RTX, args.getTransactionType());
        assertNull(args.getTransactionLabel());
        assertEquals(1, args.getNumberOfWorkerThreads());
        assertFalse(args.isVerbose());
        assertNull(args.getMonitorOutputPath());
        assertFalse(args.isVerbose());
        assertFalse(args.isPrintHelp());
        assertFalse(args.isPrintVersion());
    }

    @Test
    void parseArguments_table_multiple() {
        var app = new Main();
        var args = app.parseArguments("--connection", "ipc:testing", "A", "B", "C", "--to", "output");
        assertEquals(List.of("A", "B", "C"), args.getTableNames());
    }

    @Test
    void parseArguments_table_empty() {
        var app = new Main();
        assertThrows(ParameterException.class,
                () -> app.parseArguments("--connection", "ipc:testing", "--to", "output", ""));
    }

    @Test
    void parseArguments_table_missing() {
        var app = new Main();
        assertThrows(ParameterException.class,
                () -> app.parseArguments("--connection", "ipc:testing", "--to", "output"));
    }

    @Test
    void parseArguments_single() {
        var app = new Main();
        var args = app.parseArguments("--connection", "ipc:testing", "--single", "A", "--to", "output");
        assertTrue(args.isSingleMode());
        assertEquals(List.of("A"), args.getTableNames());
    }

    @Test
    void parseArguments_single_multiple() {
        var app = new Main();
        assertThrows(ParameterException.class,
                () -> app.parseArguments("--connection", "ipc:testing", "--single", "A", "B", "--to", "output"));
    }

    @Test
    void parseArguments_query() {
        var app = new Main();
        var args = app.parseArguments("--connection", "ipc:testing", "--sql", "A", "B", "C", "--to", "output");
        assertTrue(args.isQueryMode());
        assertEquals(List.of("A", "B", "C"), args.getTableNames());
    }

    @Test
    void parseArguments_connection_unsupported() {
        var app = new Main();
        assertThrows(ParameterException.class,
                () -> app.parseArguments("--connection", "invalid:testing", "--to", "output", "A"));
    }

    @Test
    void parseArguments_connection_missing() {
        var app = new Main();
        assertThrows(ParameterException.class,
                () -> app.parseArguments("A", "--to", "output"));
    }

    @Test
    void parseArguments_destination_missing() {
        var app = new Main();
        assertThrows(ParameterException.class,
                () -> app.parseArguments("--connection", "ipc:testing", "A"));
    }

    @Test
    void parseArguments_profile() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--profile", "TESTING");
        assertEquals(Path.of("TESTING"), args.getProfile());
        assertEquals(1, args.getProfile().getNameCount());
    }

    @Test
    void parseArguments_connection_label() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--connection-label", "TESTING");
        assertEquals("TESTING", args.getConnectionLabel());
    }

    @Test
    void parseArguments_connection_timeout() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--connection-timeout", "10000");
        assertEquals(10_000, args.getConnectionTimeoutMillis());
    }

    @Test
    void parseArguments_connection_timeout_disabled() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--connection-timeout", "0");
        assertEquals(0, args.getConnectionTimeoutMillis());
    }

    @Test
    void parseArguments_connection_timeout_out_of_range() {
        var app = new Main();
        assertThrows(ParameterException.class, () -> app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--connection-timeout", "-1"));
    }

    @Test
    void parseArguments_transaction_type() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--transaction", "long");
        assertEquals(TransactionSettings.Type.LTX, args.getTransactionType());
    }

    @Test
    void parseArguments_transaction_type_occ() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--transaction", "occ");
        assertEquals(TransactionSettings.Type.OCC, args.getTransactionType());
    }

    @Test
    void parseArguments_transaction_type_ltx() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--transaction", "ltx");
        assertEquals(TransactionSettings.Type.LTX, args.getTransactionType());
    }

    @Test
    void parseArguments_transaction_type_rtx() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--transaction", "rtx");
        assertEquals(TransactionSettings.Type.RTX, args.getTransactionType());
    }

    @Test
    void parseArguments_transaction_type_unsupported() {
        var app = new Main();
        assertThrows(ParameterException.class, () -> app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--transaction", "INVALID"));
    }

    @Test
    void parseArguments_transaction_label() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--transaction-label", "TESTING");
        assertEquals("TESTING", args.getTransactionLabel());
    }

    @Test
    void parseArguments_threads() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--threads", "16");
        assertEquals(16, args.getNumberOfWorkerThreads());
    }

    @Test
    void parseArguments_threads_invalid() {
        var app = new Main();
        assertThrows(ParameterException.class, () -> app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--threads", "0"));
    }

    @Test
    void parseArguments_monitor() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--monitor", "monitor.jsonl");
        assertEquals(Path.of("monitor.jsonl"), args.getMonitorOutputPath());
    }

    @Test
    void parseArguments_verbose() {
        var app = new Main();
        var args = app.parseArguments(
                "--connection", "ipc:testing", "A", "--to", "output",
                "--verbose");
        assertTrue(args.isVerbose());
    }

    @Test
    void parseArguments_help() {
        var app = new Main();
        var args = app.parseArguments("--help");
        assertTrue(args.isPrintHelp());
    }

    @Test
    void parseArguments_version() {
        var app = new Main();
        var args = app.parseArguments("--version");
        assertTrue(args.isPrintVersion());
    }
}
