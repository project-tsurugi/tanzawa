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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tools.tgdump.core.model.DumpTarget;

class DumpTargetSelectorTest {

    @Test
    void normalize_identity() {
        var selector = new DumpTargetSelector();
        assertEquals("testing", selector.normalize("testing"));
    }

    @Test
    void normalize_iso_control() {
        var selector = new DumpTargetSelector();
        assertEquals("testing____", selector.normalize("testing\0\t\r\n"));
    }

    @Test
    void normalize_supplementary() {
        var selector = new DumpTargetSelector();
        assertEquals("testing_", selector.normalize("testing" + String.valueOf(Character.toChars(0x1F600))));
    }

    @Test
    void normalize_whitespace() {
        var selector = new DumpTargetSelector();
        assertEquals("hello_world", selector.normalize("hello world"));
    }

    @Test
    void normalize_delimiter() {
        var selector = new DumpTargetSelector(100, "", '@', ':');
        assertEquals("hello@world", selector.normalize("hello:world"));
    }

    @Test
    void normalize_escape_targets() {
        var selector = new DumpTargetSelector(100, "[]", '@', ':');
        assertEquals("hello@3@", selector.normalize("hello[3]"));
    }

    @Test
    void normalize_upper_case() {
        var selector = new DumpTargetSelector();
        assertEquals("testing", selector.normalize("TESTING"));
    }

    @Test
    void normalize_escape_shorten() {
        var selector = new DumpTargetSelector(10, "[]", '@', ':');
        assertEquals("a".repeat(10), selector.normalize("a".repeat(100)));
    }

    @Test
    void getTargets_simple() {
        var selector = new DumpTargetSelector();
        var targets = selector.getTargets(Path.of("p"), List.of("a"));
        assertEquals(Map.of("a", "p/a"), toMap(targets));
    }

    @Test
    void getTargets_multiple() {
        var selector = new DumpTargetSelector();
        var targets = selector.getTargets(Path.of("p"), List.of("a", "b", "c"));
        assertEquals(Map.of("a", "p/a", "b", "p/b", "c", "p/c"), toMap(targets));
    }

    @Test
    void getTargets_conflict() {
        var selector = new DumpTargetSelector();
        var targets = selector.getTargets(Path.of("p"), List.of("a", "A"));
        assertEquals(Map.of("a", "p/a", "A", "p/a-*"), toMap(targets));
    }

    @Test
    void getTargets_conflict_multiple() {
        var selector = new DumpTargetSelector();
        var targets = selector.getTargets(Path.of("p"), List.of("a", "A", "a_", "A_", "A?"));
        assertEquals(Map.of(
                "a", "p/a",
                "A", "p/a-*",
                "a_", "p/a_",
                "A_", "p/a_-*",
                "A?", "p/a_-*"), toMap(targets));
    }

    @Test
    void equivalent() {
        var a = new DumpTargetSelector();
        var b = new DumpTargetSelector();
        assertEquals(a, b, String.format("%s = %s", a, b));
    }

    private static Map<String, String> toMap(List<DumpTarget> targets) {
        // SELECT destination
        //   GROUP BY destination
        //   HAVING count(*) >= 2
        //   ORDER BY destination
        var conflicts = targets.stream()
            .map(DumpTarget::getDestination)
            .collect(Collectors.groupingBy(k -> k, Collectors.counting()))
            .entrySet().stream()
            .filter(e -> e.getValue() >= 2)
            .map(e -> e.getKey())
            .sorted()
            .collect(Collectors.toList());
        assertEquals(List.of(), conflicts);
        return targets.stream()
                .collect(Collectors.toMap(
                        DumpTarget::getTableName,
                        t -> {
                            var r = t.getDestination().toString().replace(File.separatorChar, '/');
                            // replace suffix with "-*"
                            return r.replaceFirst("-\\d+$", "-*");
                        }));
    }
}
