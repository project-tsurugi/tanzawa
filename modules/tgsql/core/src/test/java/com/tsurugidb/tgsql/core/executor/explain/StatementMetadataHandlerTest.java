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
package com.tsurugidb.tgsql.core.executor.explain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.tsurugidb.tgsql.core.executor.engine.EngineConfigurationException;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.executor.engine.TestUtil;
import com.tsurugidb.tgsql.core.executor.report.TestReporter;
import com.tsurugidb.tgsql.core.model.ErroneousStatement;
import com.tsurugidb.tgsql.core.model.Region;
import com.tsurugidb.tgsql.core.model.Regioned;
import com.tsurugidb.tgsql.core.model.Value;
import com.tsurugidb.tsubakuro.explain.PlanNode;
import com.tsurugidb.tsubakuro.explain.json.JsonPlanGraphLoader;
import com.tsurugidb.tsubakuro.sql.impl.BasicStatementMetadata;

class StatementMetadataHandlerTest {

    private Path temporary;

    @BeforeEach
    void prepareTempDir() throws IOException {
        temporary = TestUtil.createTempDir();
    }

    @AfterEach
    void cleanupTempDir() throws IOException {
        TestUtil.removeDir(temporary);
    }

    @Test
    void fromOptions_empty() throws Exception {
        var handler = StatementMetadataHandler.fromOptions(Map.of());
        var reporter = new TestReporter();
        var plan = handler.handle(reporter, new BasicStatementMetadata(
                JsonPlanGraphLoader.SUPPORTED_FORMAT_ID,
                JsonPlanGraphLoader.SUPPORTED_FORMAT_VERSION_MAX,
                TestUtil.read("explain-find-project-write.json"),
                List.of()));
        assertTrue(plan.getNodes().stream()
                .map(PlanNode::getKind)
                .anyMatch(Predicate.isEqual("find")));
        assertFalse(plan.getNodes().stream()
                .map(PlanNode::getKind)
                .anyMatch(Predicate.isEqual("project")));
        assertTrue(plan.getNodes().stream()
                .map(PlanNode::getKind)
                .anyMatch(Predicate.isEqual("write")));
    }

    @Test
    void fromOptions_verbose() throws Exception {
        var handler = StatementMetadataHandler.fromOptions(
                toOptions(Map.of(
                        StatementMetadataHandler.KEY_VERBOSE,
                        Optional.empty())));
        var reporter = new TestReporter();
        var plan = handler.handle(reporter, new BasicStatementMetadata(
                JsonPlanGraphLoader.SUPPORTED_FORMAT_ID,
                JsonPlanGraphLoader.SUPPORTED_FORMAT_VERSION_MAX,
                TestUtil.read("explain-find-project-write.json"),
                List.of()));
        assertTrue(plan.getNodes().stream()
                .map(PlanNode::getKind)
                .anyMatch(Predicate.isEqual("find")));
        assertTrue(plan.getNodes().stream()
                .map(PlanNode::getKind)
                .anyMatch(Predicate.isEqual("project")));
        assertTrue(plan.getNodes().stream()
                .map(PlanNode::getKind)
                .anyMatch(Predicate.isEqual("write")));
    }

    @Test
    void fromOptions_verbose_true() throws Exception {
        var handler = StatementMetadataHandler.fromOptions(
                toOptions(Map.of(
                        StatementMetadataHandler.KEY_VERBOSE,
                        Optional.of(Value.of(true)))));
        var reporter = new TestReporter();
        var plan = handler.handle(reporter, new BasicStatementMetadata(
                JsonPlanGraphLoader.SUPPORTED_FORMAT_ID,
                JsonPlanGraphLoader.SUPPORTED_FORMAT_VERSION_MAX,
                TestUtil.read("explain-find-project-write.json"),
                List.of()));
        assertTrue(plan.getNodes().stream()
                .map(PlanNode::getKind)
                .anyMatch(Predicate.isEqual("find")));
        assertTrue(plan.getNodes().stream()
                .map(PlanNode::getKind)
                .anyMatch(Predicate.isEqual("project")));
        assertTrue(plan.getNodes().stream()
                .map(PlanNode::getKind)
                .anyMatch(Predicate.isEqual("write")));
    }

    @Test
    void fromOptions_verbose_invalid() throws Exception {
        var e = assertThrows(EngineConfigurationException.class, () -> StatementMetadataHandler.fromOptions(
                toOptions(Map.of(
                        StatementMetadataHandler.KEY_VERBOSE,
                        Optional.of(Value.of())))));
        assertEquals(ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION, e.getErrorKind());
    }

    @Test
    void fromOptions_output() throws Exception {
        var output = temporary.resolve("output").resolve("plan.json");
        var handler = StatementMetadataHandler.fromOptions(
                toOptions(Map.of(
                        StatementMetadataHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toString())))));
        var reporter = new TestReporter();
        var json = TestUtil.read("explain-find-project-write.json");
        handler.handle(reporter, new BasicStatementMetadata(
                JsonPlanGraphLoader.SUPPORTED_FORMAT_ID,
                JsonPlanGraphLoader.SUPPORTED_FORMAT_VERSION_MAX,
                json,
                List.of()));

        var lines = Files.readAllLines(output);
        assertEquals(
                Arrays.asList(json.split("\\r?\\n")),
                lines);
    }

    @Test
    void fromOptions_output_invalid() throws Exception {
        var e = assertThrows(EngineConfigurationException.class, () -> StatementMetadataHandler.fromOptions(
                toOptions(Map.of(
                        StatementMetadataHandler.KEY_OUTPUT,
                        Optional.of(Value.of(100))))));
        assertEquals(ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION, e.getErrorKind());
    }

    @Test
    void fromOptions_output_conflict() throws Exception {
        var output = temporary.resolve("output");
        Files.createDirectories(output);
        var e = assertThrows(EngineConfigurationException.class, () -> StatementMetadataHandler.fromOptions(
                toOptions(Map.of(
                        StatementMetadataHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toString()))))));
        assertEquals(ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION, e.getErrorKind());
    }

    @Test
    void isHandled() throws Exception {
        var handler = StatementMetadataHandler.fromOptions(Map.of());
        assertTrue(handler.isHandled(StatementMetadataHandler.KEY_OUTPUT));
        assertTrue(handler.isHandled(StatementMetadataHandler.KEY_VERBOSE));
        assertTrue(handler.isHandled(StatementMetadataHandler.KEY_OUTPUT.toUpperCase(Locale.ENGLISH)));
    }

    @Test
    void handle_json_not_supported() throws Exception {
        var handler = StatementMetadataHandler.fromOptions(Map.of());
        var reporter = new TestReporter();
        assertThrows(EngineException.class, () -> handler.handle(reporter, new BasicStatementMetadata(
                JsonPlanGraphLoader.SUPPORTED_FORMAT_ID,
                JsonPlanGraphLoader.SUPPORTED_FORMAT_VERSION_MAX,
                "?",
                List.of())));
    }

    @Test
    void handle_json_broken() throws Exception {
        var handler = StatementMetadataHandler.fromOptions(Map.of());
        var reporter = new TestReporter();
        assertThrows(EngineException.class, () -> handler.handle(reporter, new BasicStatementMetadata(
                "__INVALID_FORMAT_ID__",
                JsonPlanGraphLoader.SUPPORTED_FORMAT_VERSION_MAX,
                TestUtil.read("explain-find-project-write.json"),
                List.of())));
    }

    @Test
    void handle_output_failure() throws Exception {
        var output = temporary.resolve("plan.json");
        var handler = StatementMetadataHandler.fromOptions(
                toOptions(Map.of(
                        StatementMetadataHandler.KEY_OUTPUT,
                        Optional.of(Value.of(output.toString())))));
        var reporter = new TestReporter();
        Files.createDirectories(output);
        assertThrows(EngineException.class, () -> handler.handle(reporter, new BasicStatementMetadata(
                JsonPlanGraphLoader.SUPPORTED_FORMAT_ID,
                JsonPlanGraphLoader.SUPPORTED_FORMAT_VERSION_MAX,
                TestUtil.read("explain-find-project-write.json"),
                List.of())));
    }

    private static Map<Regioned<String>, Optional<Regioned<Value>>> toOptions(Map<String, Optional<Value>> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> new Region(0, 0, 0, 0).wrap(entry.getKey()),
                        entry -> entry.getValue().map(it -> new Region(0, 0, 0, 0).wrap(it))));
    }
}
