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
package com.tsurugidb.tgsql.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tgsql.core.config.TgsqlCvKey.TgsqlCvKeyBoolean;

class TgsqlCvKeyBooleanTest {

    @Test
    void convertValue() {
        var key = new TgsqlCvKeyBoolean("test");
        assertEquals(Boolean.FALSE, key.convertValue(""));
        assertEquals(Boolean.FALSE, key.convertValue(" "));

        assertEquals(Boolean.TRUE, key.convertValue("t"));
        assertEquals(Boolean.TRUE, key.convertValue("tr"));
        assertEquals(Boolean.TRUE, key.convertValue("tru"));
        assertEquals(Boolean.TRUE, key.convertValue("true"));
        assertEquals(Boolean.FALSE, key.convertValue("truee"));

        assertEquals(Boolean.TRUE, key.convertValue("T"));
        assertEquals(Boolean.TRUE, key.convertValue("TR"));
        assertEquals(Boolean.TRUE, key.convertValue("TRU"));
        assertEquals(Boolean.TRUE, key.convertValue("TRUE"));
        assertEquals(Boolean.FALSE, key.convertValue("TRUEE"));

        assertEquals(Boolean.TRUE, key.convertValue("1"));
        assertEquals(Boolean.FALSE, key.convertValue("0"));
    }
}
