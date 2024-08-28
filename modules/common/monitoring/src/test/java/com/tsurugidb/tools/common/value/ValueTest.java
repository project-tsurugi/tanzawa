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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ValueTest {

    @Test
    void ofNull() {
        var v = Value.ofNull();
        assertEquals(Value.Kind.NULL, v.getKind());
        assertNull(v.getEntity());
        assertTrue(v.isNull());
        assertEquals("null", v.toString());
        assertEquals(v, Value.fromObject(null));
    }

    @Test
    void of_boolean() {
        var v = Value.of(true);
        assertEquals(Value.Kind.BOOLEAN, v.getKind());
        assertEquals(true, v.getEntity());
        assertTrue(v.asBoolean());
        assertEquals("true", v.toString());
        assertEquals(v, Value.fromObject(true));
    }

    @Test
    void of_int() {
        var v = Value.of(100);
        assertEquals(Value.Kind.INTEGER, v.getKind());
        assertEquals(100L, v.getEntity());
        assertEquals(100L, v.asInteger());
        assertEquals("100", v.toString());
        assertEquals(v, Value.fromObject(100));
    }

    @Test
    void of_long() {
        var v = Value.of(200L);
        assertEquals(Value.Kind.INTEGER, v.getKind());
        assertEquals(200L, v.getEntity());
        assertEquals(200L, v.asInteger());
        assertEquals("200", v.toString());
        assertEquals(v, Value.fromObject(200L));
    }

    @Test
    void of_BigDecimal() {
        var v = Value.of(new BigDecimal("3.14"));
        assertEquals(Value.Kind.DECIMAL, v.getKind());
        assertEquals(new BigDecimal("3.14"), v.getEntity());
        assertEquals(new BigDecimal("3.14"), v.asDecimal());
        assertEquals("3.14", v.toString());
        assertEquals(v, Value.fromObject(new BigDecimal("3.14")));
    }

    @Test
    void of_String() {
        var v = Value.of("TESTING");
        assertEquals(Value.Kind.STRING, v.getKind());
        assertEquals("TESTING", v.getEntity());
        assertEquals("TESTING", v.asString());
        assertEquals("TESTING", v.toString());
        assertEquals(v, Value.fromObject("TESTING"));
    }

    @Test
    void of_Array() {
        var e = Array.of(1, 2, 3);
        var v = Value.of(e);
        assertEquals(Value.Kind.ARRAY, v.getKind());
        assertEquals(e, v.getEntity());
        assertEquals(e, v.asArray());
        assertEquals(e.toString(), v.toString());
        assertEquals(v, Value.fromObject(e));
    }

    @Test
    void of_Record() {
        var e = Record.of(
                Property.of("a", Value.of(1)),
                Property.of("b", Value.of(2)),
                Property.of("c", Value.of(3)));
        var v = Value.of(e);
        assertEquals(Value.Kind.RECORD, v.getKind());
        assertEquals(e, v.getEntity());
        assertEquals(e, v.asRecord());
        assertEquals(e.toString(), v.toString());
        assertEquals(v, Value.fromObject(e));
    }

    @Test
    void fromObject_itself() {
        assertEquals(Value.of(1), Value.fromObject(Value.of(1)));
    }

    @Test
    void fromObject_invalid() {
        assertThrows(IllegalArgumentException.class, () -> Value.fromObject(new Object()));
    }

    @Test
    void as_invalid() {
        assertThrows(IllegalStateException.class, () -> Value.of(true).asInteger());
    }
}
