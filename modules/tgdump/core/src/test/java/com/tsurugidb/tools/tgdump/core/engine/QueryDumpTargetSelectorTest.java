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
package com.tsurugidb.tools.tgdump.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tools.tgdump.core.model.DumpTarget;

class QueryDumpTargetSelectorTest {

    private static QueryDumpTargetSelector selector() {
        return new QueryDumpTargetSelector();
    }

    private static QueryDumpTargetSelector.LabelAndStatement parsed(String label, String statement) {
        return new QueryDumpTargetSelector.LabelAndStatement(label, statement);
    }

    @Test
    void parseCommand_simple() {
        var result = selector().parseCommand("q:SELECT 1");
        assertEquals(parsed("q", "SELECT 1"), result);
    }

    @Test
    void parseCommand_empty() {
        var result = selector().parseCommand("");
        assertEquals(parsed(null, ""), result);
    }

    @Test
    void parseCommand_word() {
        var result = selector().parseCommand("query:SELECT 1");
        assertEquals(parsed("query", "SELECT 1"), result);
    }

    @Test
    void parseCommand_default() {
        var result = selector().parseCommand("SELECT 1");
        assertEquals(parsed(null, "SELECT 1"), result);
    }

    @Test
    void parseCommand_spaces() {
        var result = selector().parseCommand("  q  :  SELECT 1");
        assertEquals(parsed("q", "SELECT 1"), result);
    }

    @Test
    void parseCommand_empty_label() {
        var result = selector().parseCommand(":SELECT 1");
        assertEquals(parsed("", "SELECT 1"), result);
    }

    @Test
    void parseCommand_empty_label_spaces() {
        var result = selector().parseCommand("  :  SELECT 1");
        assertEquals(parsed("", "SELECT 1"), result);
    }

    @Test
    void parseCommand_stop() {
        var result = selector().parseCommand("'a':SELECT 1");
        assertEquals(parsed(null, "'a':SELECT 1"), result);
    }

    @Test
    void parseCommand_control() {
        var result = selector().parseCommand("a\0:SELECT 1");
        assertEquals(parsed(null, "a\0:SELECT 1"), result);
    }

    @Test
    void parseCommand_body_stop() {
        var result = selector().parseCommand("q'a':SELECT 1");
        assertEquals(parsed(null, "q'a':SELECT 1"), result);
    }

    @Test
    void parseCommand_padding_stop() {
        var result = selector().parseCommand("q 'a':SELECT 1");
        assertEquals(parsed(null, "q 'a':SELECT 1"), result);
    }

    @Test
    void parseCommand_empty_statement() {
        var result = selector().parseCommand("q:");
        assertEquals(parsed("q", ""), result);
    }

    @Test
    void parseCommand_empty_command() {
        var result = selector().parseCommand("");
        assertEquals(parsed(null, ""), result);
    }

    @Test
    void getTarget_simple() {
        var result = selector().getTarget(Path.of("p"), "q:SELECT 1");
        assertEquals(DumpTarget.TargetType.QUERY, result.getTargetType());
        assertEquals("q", result.getLabel());
        assertEquals("SELECT 1", result.getTarget());
        assertEquals("p", toDestinationString(result));
    }

    @Test
    void getTarget_default_label() {
        var result = selector().getTarget(Path.of("p"), "SELECT 1");
        assertEquals(QueryDumpTargetSelector.DEFAULT_DEFAULT_PREFIX, result.getLabel());
        assertEquals("SELECT 1", result.getTarget());
        assertEquals("p", toDestinationString(result));
    }

    @Test
    void getTarget_empty() {
        assertThrows(IllegalArgumentException.class, () -> selector().getTarget(Path.of("p"), ""));
    }

    @Test
    void getTarget_empty_label() {
        assertThrows(IllegalArgumentException.class, () -> selector().getTarget(Path.of("p"), ":SELECT 1"));
    }

    @Test
    void getTarget_empty_statement() {
        assertThrows(IllegalArgumentException.class, () -> selector().getTarget(Path.of("p"), "q:"));
    }

    @Test
    void getTargets_simple() {
        var results = selector().getTargets(Path.of("p"), List.of("q:SELECT 1"));
        assertEquals(Map.of("q:SELECT 1", "p/q"), toMap(results));
    }

    @Test
    void getTargets_multiple() {
        var results = selector().getTargets(Path.of("p"), List.of("q1:SELECT 1", "q2:SELECT 2", "q3:SELECT 3"));
        assertEquals(Map.of(
                "q1:SELECT 1", "p/q1",
                "q2:SELECT 2", "p/q2",
                "q3:SELECT 3", "p/q3"), toMap(results));
    }

    @Test
    void getTargets_default_label() {
        var results = selector().getTargets(Path.of("p"), List.of("SELECT 1"));
        assertEquals(Map.of("sql1:SELECT 1", "p/sql1"), toMap(results));
    }

    @Test
    void getTargets_default_label_multiple() {
        var results = selector().getTargets(Path.of("p"), List.of("SELECT 1", "SELECT 2", "SELECT 3"));
        assertEquals(Map.of(
                "sql1:SELECT 1", "p/sql1",
                "sql2:SELECT 2", "p/sql2",
                "sql3:SELECT 3", "p/sql3"), toMap(results));
    }

    @Test
    void getTargets_conflict_label() {
        var results = selector().getTargets(Path.of("p"), List.of("q:SELECT 1", "q:SELECT 2"));
        assertEquals(Map.of(
                "q:SELECT 1", "p/q-1",
                "q:SELECT 2", "p/q-2"), toMap(results));
    }

    @Test
    void getTargets_empty_label() {
        assertThrows(IllegalArgumentException.class, () -> selector().getTargets(Path.of("p"), List.of(":SELECT 1")));
    }

    @Test
    void getTargets_empty_statement() {
        assertThrows(IllegalArgumentException.class, () -> selector().getTargets(Path.of("p"), List.of("q:")));
    }

    @Test
    void equivalent() {
        var a = selector();
        var b = selector();
        assertEquals(a, b, String.format("%s = %s", a, b));
    }

    private static Map<String, String> toMap(List<DumpTarget> targets) {
        return targets.stream()
                .peek(it -> assertEquals(DumpTarget.TargetType.QUERY, it.getTargetType()))
                .peek(it -> assertEquals(it.getLabel(), it.getLabel()))
                .peek(it -> assertEquals(it.getTarget(), it.getTarget()))
                .collect(Collectors.toMap(
                        t -> String.format("%s:%s", t.getLabel(), t.getTarget()),
                        QueryDumpTargetSelectorTest::toDestinationString));
    }

    private static String toDestinationString(DumpTarget target) {
        return target.getDestination().toString().replace(File.separatorChar, '/');
    }
}
