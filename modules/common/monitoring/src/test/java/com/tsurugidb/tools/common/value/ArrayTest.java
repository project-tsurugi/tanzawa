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
package com.tsurugidb.tools.common.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ArrayTest {

    @Test
    void ctor_empty() {
        var e = new Array();
        assertEquals(List.of(), e.getElements());
        assertEquals(List.of().toString(), e.toString());
    }

    @Test
    void ctor_list() {
        var elements = List.of(Value.of(1), Value.of(2), Value.of(3));
        var e = new Array(elements);
        assertEquals(elements, e.getElements());
        assertEquals(elements.toString(), e.toString());
    }

    @Test
    void of() {
        var e = Array.of(1, 2, 3);
        assertEquals(new Array(List.of(Value.of(1), Value.of(2), Value.of(3))), e);
    }

    @Test
    void of_invalid() {
        assertThrows(IllegalArgumentException.class, () -> Array.of(new Object()));
    }

    @Test
    void iterator() {
        var e = Array.of(1, 2, 3);

        var r = new ArrayList<Value>();
        e.iterator().forEachRemaining(r::add);
        assertEquals(e.getElements(), r);
    }
}
