package com.tsurugidb.console.core.executor.explain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tsurugidb.console.core.executor.engine.CommandPath;
import com.tsurugidb.console.core.executor.engine.EngineConfigurationException;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.executor.engine.TestUtil;
import com.tsurugidb.console.core.executor.report.BasicReporter;
import com.tsurugidb.console.core.model.ErroneousStatement;
import com.tsurugidb.console.core.model.Region;
import com.tsurugidb.console.core.model.Regioned;
import com.tsurugidb.console.core.model.Value;
import com.tsurugidb.tsubakuro.explain.BasicPlanGraph;
import com.tsurugidb.tsubakuro.explain.BasicPlanNode;

class DotOutputHandlerTest {

    private static final String KEY_DOT_EXECUTABLE = "tanzawa.dot";

    private Path temporary;

    @BeforeEach
    void prepareTempDir() throws IOException {
        temporary = TestUtil.createTempDir();
    }

    @AfterEach
    void cleanupTempDir() throws IOException {
        TestUtil.removeDir(temporary);
    }

    Path assumeDotCommand() {
        var executable = Optional.ofNullable(System.getProperty(KEY_DOT_EXECUTABLE))
                .map(String::trim)
                .filter(it -> !it.isEmpty())
                .orElse(null);
        assumeTrue(executable != null, MessageFormat.format(
                "dot command is not specified: {0}",
                KEY_DOT_EXECUTABLE));
        return Path.of(executable).toAbsolutePath();
    }

    private BasicPlanGraph getSimpleGraph() {
        var a = new BasicPlanNode("a", Map.of());
        var b = new BasicPlanNode("b", Map.of());
        a.addDownstream(b);
        var graph = new BasicPlanGraph(List.of(a, b));
        return graph;
    }

    @Test
    void fromOptions_no_options() throws Exception {
        var handler = DotOutputHandler.fromOptions(Map.of(), new CommandPath(List.of()));
        var graph = getSimpleGraph();
        var reporter = new BasicReporter();

        handler.handle(reporter, graph);

        // ok
    }

    @Test
    void fromOptions_output_raw() throws Exception {
        var output = temporary.resolve("output").resolve("out.dot");
        var handler = DotOutputHandler.fromOptions(
                toOptions(Map.of(
                        DotOutputHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toAbsolutePath().toString())))),
                new CommandPath(List.of()));

        var graph = getSimpleGraph();
        var reporter = new BasicReporter();
        handler.handle(reporter, graph);

        assertTrue(Files.exists(output));
    }

    @Test
    void fromOptions_output_svg() throws Exception {
        var output = temporary.resolve("output").resolve("out.svg");
        var handler = DotOutputHandler.fromOptions(
                toOptions(Map.of(
                        DotOutputHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toAbsolutePath().toString())),
                        DotOutputHandler.KEY_EXECUTABLE,
                        Optional.of(Value.of(assumeDotCommand().toString())))),
                new CommandPath(List.of()));

        var graph = getSimpleGraph();

        var reporter = new BasicReporter();
        handler.handle(reporter, graph);

        assertTrue(Files.exists(output));
    }

    @Test
    void fromOptions_output_invalid() throws Exception {
        var output = temporary.resolve("output");
        Files.createDirectories(output);
        var e = assertThrows(EngineConfigurationException.class, () -> DotOutputHandler.fromOptions(
                toOptions(Map.of(
                        DotOutputHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toAbsolutePath().toString())))),
                new CommandPath(List.of())));
        assertEquals(ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION, e.getErrorKind());
    }

    @Test
    void fromOptions_verbose() throws Exception {
        var output = temporary.resolve("out.dot");
        var handler = DotOutputHandler.fromOptions(
                toOptions(Map.of(
                        DotOutputHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toAbsolutePath().toString())),
                        DotOutputHandler.KEY_VERBOSE,
                        Optional.empty())),
                new CommandPath(List.of()));

        var graph = getSimpleGraph();
        var reporter = new BasicReporter();
        handler.handle(reporter, graph);

        assertTrue(Files.exists(output));
    }

    @Test
    void fromOptions_verbose_true() throws Exception {
        var output = temporary.resolve("out.dot");
        var handler = DotOutputHandler.fromOptions(
                toOptions(Map.of(
                        DotOutputHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toAbsolutePath().toString())),
                        DotOutputHandler.KEY_VERBOSE,
                        Optional.of(Value.of(true)))),
                new CommandPath(List.of()));

        var graph = getSimpleGraph();
        var reporter = new BasicReporter();
        handler.handle(reporter, graph);

        assertTrue(Files.exists(output));
    }

    @Test
    void fromOptions_executable_default() throws Exception {
        var dot = assumeDotCommand();
        var output = temporary.resolve("out.svg");
        var handler = DotOutputHandler.fromOptions(
                toOptions(Map.of(
                        DotOutputHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toAbsolutePath().toString())))),
                new CommandPath(List.of(dot.toAbsolutePath().getParent())));

        var graph = getSimpleGraph();

        var reporter = new BasicReporter();
        handler.handle(reporter, graph);

        assertTrue(Files.exists(output));
    }

    @Test
    void fromOptions_executable_not_specified() throws Exception {
        var output = temporary.resolve("out.svg");
        var e = assertThrows(EngineConfigurationException.class, () -> DotOutputHandler.fromOptions(
                toOptions(Map.of(
                        DotOutputHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toAbsolutePath().toString())))),
                new CommandPath(List.of())));
        assertEquals(ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION, e.getErrorKind());
    }

    @Test
    void fromOptions_executable_not_found() throws Exception {
        var output = temporary.resolve("out.svg");
        var e = assertThrows(EngineConfigurationException.class, () -> DotOutputHandler.fromOptions(
                toOptions(Map.of(
                        DotOutputHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toAbsolutePath().toString())),
                        DotOutputHandler.KEY_EXECUTABLE,
                        Optional.of(Value.of("__INVALID_EXECUTABLE__")))),
                new CommandPath(List.of())));
        assertEquals(ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION, e.getErrorKind());
    }

    @Test
    void handle_output_failure() throws Exception {
        var output = temporary.resolve("out.dot");
        var handler = DotOutputHandler.fromOptions(
                toOptions(Map.of(
                        DotOutputHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toAbsolutePath().toString())))),
                new CommandPath(List.of()));

        Files.createDirectories(output);

        var graph = getSimpleGraph();
        var reporter = new BasicReporter();
        assertThrows(EngineException.class, () -> handler.handle(reporter, graph));
    }

    @Test
    void isHandled_empty() throws Exception {
        var handler = DotOutputHandler.fromOptions(Map.of(), new CommandPath(List.of()));
        assertTrue(handler.isHandled(DotOutputHandler.KEY_OUTPUT));
        assertTrue(handler.isHandled(DotOutputHandler.KEY_VERBOSE));
        assertTrue(handler.isHandled(DotOutputHandler.KEY_EXECUTABLE));
        assertTrue(handler.isHandled(DotOutputHandler.KEY_OUTPUT.toUpperCase(Locale.ENGLISH)));
    }

    @Test
    void isHandled_configured() throws Exception {
        var output = temporary.resolve("out.dot");
        var handler = DotOutputHandler.fromOptions(
                toOptions(Map.of(
                        DotOutputHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toAbsolutePath().toString())))),
                new CommandPath(List.of()));
        assertTrue(handler.isHandled(DotOutputHandler.KEY_OUTPUT));
        assertTrue(handler.isHandled(DotOutputHandler.KEY_VERBOSE));
        assertTrue(handler.isHandled(DotOutputHandler.KEY_EXECUTABLE));
        assertTrue(handler.isHandled(DotOutputHandler.KEY_OUTPUT.toUpperCase(Locale.ENGLISH)));
    }

    @Test
    void createCommandLine_default() {
        var options = DotOutputHandler.extendOptions(Map.of(), Map.of());
        var commandLine = DotOutputHandler.createCommandLine("dot", "pdf", options);
        var expected = List.of(
                "dot",
                "-Tpdf",
                "-Grankdir=RL",
                "-Nshape=rect");
        assertLinesMatch(expected, commandLine);
    }

    @Test
    void createCommandLine_option() {
        var options = DotOutputHandler.extendOptions(
                toOptions(Map.of(DotOutputHandler.KEY_GRAPH_PREFIX + "rankdir", Optional.of(Value.of("TB")))),
                Map.of());
        var commandLine = DotOutputHandler.createCommandLine("dot", "pdf", options);
        var expected = List.of(
                "dot",
                "-Tpdf",
                "-Grankdir=TB",
                "-Nshape=rect");
        assertLinesMatch(expected, commandLine);
    }

    @Test
    void createCommandLine_config() {
        var options = DotOutputHandler.extendOptions(
                Map.of(),
                Map.of(DotOutputHandler.KEY_GRAPH_PREFIX + "rankdir", "TB"));
        var commandLine = DotOutputHandler.createCommandLine("dot", "pdf", options);
        var expected = List.of(
                "dot",
                "-Tpdf",
                "-Grankdir=TB",
                "-Nshape=rect");
        assertLinesMatch(expected, commandLine);
    }

    private static Map<Regioned<String>, Optional<Regioned<Value>>> toOptions(Map<String, Optional<Value>> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> new Region(0, 0, 0, 0).wrap(entry.getKey()),
                        entry -> entry.getValue().map(it -> new Region(0, 0, 0, 0).wrap(it))));
    }
}
