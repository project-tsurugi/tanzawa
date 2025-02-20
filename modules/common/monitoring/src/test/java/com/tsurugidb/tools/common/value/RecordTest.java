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
package com.tsurugidb.tools.common.value;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class RecordTest {

    @Test
    void ctor_empty() {
        var e = new Record();
        assertEquals(List.of(), e.getProperties());
        assertEquals("{}", e.toString());
    }

    @Test
    void ctor_list() {
        var props = List.of(
                Property.of("a", Value.of(1)),
                Property.of("b", Value.of(2)),
                Property.of("c", Value.of(3)));
        var e = new Record(props);
        assertEquals(props, e.getProperties());
        assertEquals("{a:1, b:2, c:3}", e.toString());
    }

    @Test
    void of() {
        var props = List.of(
                Property.of("a", Value.of(1)),
                Property.of("b", Value.of(2)),
                Property.of("c", Value.of(3)));
        var e = Record.of(props.toArray(new Property[props.size()]));
        assertEquals(new Record(props), e);
    }

    @Test
    void iterator() {
        var e = Record.of(
                Property.of("a", Value.of(1)),
                Property.of("b", Value.of(2)),
                Property.of("c", Value.of(3)));

        var r = new ArrayList<Property>();
        e.iterator().forEachRemaining(r::add);
        assertEquals(e.getProperties(), r);
    }
}
