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

import org.junit.jupiter.api.Test;

class NameNormalizerTest {

    @Test
    void normalize_identity() {
        var normalizer = new NameNormalizer();
        assertEquals("testing", normalizer.apply("testing"));
    }

    @Test
    void normalize_iso_control() {
        var normalizer = new NameNormalizer();
        assertEquals("testing____", normalizer.apply("testing\0\t\r\n"));
    }

    @Test
    void normalize_supplementary() {
        var normalizer = new NameNormalizer();
        assertEquals("testing_", normalizer.apply("testing" + String.valueOf(Character.toChars(0x1F600))));
    }

    @Test
    void normalize_whitespace() {
        var normalizer = new NameNormalizer();
        assertEquals("hello_world", normalizer.apply("hello world"));
    }

    @Test
    void normalize_delimiter() {
        var normalizer = new NameNormalizer(100, "", '@', ':');
        assertEquals("hello@world", normalizer.apply("hello:world"));
    }

    @Test
    void normalize_escape_targets() {
        var normalizer = new NameNormalizer(100, "[]", '@', ':');
        assertEquals("hello@3@", normalizer.apply("hello[3]"));
    }

    @Test
    void normalize_upper_case() {
        var normalizer = new NameNormalizer();
        assertEquals("testing", normalizer.apply("TESTING"));
    }

    @Test
    void normalize_escape_shorten() {
        var normalizer = new NameNormalizer(10, "[]", '@', ':');
        assertEquals("a".repeat(10), normalizer.apply("a".repeat(100)));
    }

    @Test
    void equivalent() {
        var a = new NameNormalizer();
        var b = new NameNormalizer();
        assertEquals(a, b, String.format("%s = %s", a, b));
    }
}
