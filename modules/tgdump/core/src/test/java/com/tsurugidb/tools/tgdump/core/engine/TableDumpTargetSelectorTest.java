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
package com.tsurugidb.tools.tgdump.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tools.tgdump.core.model.DumpTarget;

class TableDumpTargetSelectorTest {

    private static TableDumpTargetSelector selector() {
        return new TableDumpTargetSelector();
    }

    @Test
    void getTarget_simple() {
        var target = selector().getTarget(Path.of("p"), "a");
        assertEquals(DumpTarget.TargetType.TABLE, target.getTargetType());
        assertEquals("a", target.getTableName());
        assertEquals("p", toDestinationString(target));
    }

    @Test
    void getTarget_strip() {
        var target = selector().getTarget(Path.of("p"), "  A  ");
        assertEquals("A", target.getTableName());
        assertEquals("p", toDestinationString(target));
    }

    @Test
    void getTargets_simple() {
        var targets = selector().getTargets(Path.of("p"), List.of("a"));
        assertEquals(Map.of("a", "p/a"), toMap(targets));
    }

    @Test
    void getTargets_multiple() {
        var targets = selector().getTargets(Path.of("p"), List.of("a", "b", "c"));
        assertEquals(Map.of("a", "p/a", "b", "p/b", "c", "p/c"), toMap(targets));
    }

    @Test
    void getTargets_empty_name() {
        assertThrows(IllegalArgumentException.class, () -> selector().getTargets(Path.of("p"), List.of("")));
    }

    @Test
    void getTargets_conflict() {
        var targets = selector().getTargets(Path.of("p"), List.of("a", "A"));
        assertEquals(Map.of("a", "p/a-1", "A", "p/a-2"), toMap(targets));
    }

    @Test
    void getTargets_conflict_multiple() {
        var targets = selector().getTargets(Path.of("p"), List.of("a", "A", "a_", "A_", "A?"));
        assertEquals(Map.of(
                "a", "p/a-1",
                "A", "p/a-2",
                "a_", "p/a_-1",
                "A_", "p/a_-2",
                "A?", "p/a_-3"), toMap(targets));
    }

    @Test
    void equivalent() {
        var a = selector();
        var b = selector();
        assertEquals(a, b, String.format("%s = %s", a, b));
    }

    private static Map<String, String> toMap(List<DumpTarget> targets) {
        return targets.stream()
                .peek(it -> assertEquals(DumpTarget.TargetType.TABLE, it.getTargetType()))
                .peek(it -> assertEquals(it.getTableName(), it.getTarget()))
                .peek(it -> assertEquals(it.getTableName(), it.getLabel()))
                .collect(Collectors.toMap(
                        DumpTarget::getTableName,
                        TableDumpTargetSelectorTest::toDestinationString));
    }

    private static String toDestinationString(DumpTarget target) {
        return target.getDestination().toString().replace(File.separatorChar, '/');
    }
}
