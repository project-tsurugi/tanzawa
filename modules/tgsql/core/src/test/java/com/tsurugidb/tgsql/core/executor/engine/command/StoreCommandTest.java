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
package com.tsurugidb.tgsql.core.executor.engine.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tgsql.core.exception.TgsqlMessageException;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
import com.tsurugidb.tgsql.core.parser.SqlParser;

class StoreCommandTest {

    private final StoreCommand target = new StoreCommand();

    @Test
    void objectName() throws Exception {
        var statement = parse("blob@0 /path/to/file");

        var argument = target.parseArgument("blob", statement);
        assertEquals("blob", argument.objectPrefix);
        assertEquals(0, argument.objectNumber);
        assertEquals("/path/to/file", argument.destination);
    }

    @Test
    void subCommand_objectName() throws Exception {
        var statement = parse("blob blob@0 /path/to/file");

        var argument = target.parseArgument("blob", statement);
        assertEquals("blob", argument.objectPrefix);
        assertEquals(0, argument.objectNumber);
        assertEquals("/path/to/file", argument.destination);
    }

    @Test
    void subCommand_objectNumber() throws Exception {
        var statement = parse("blob 0 /path/to/file");

        var argument = target.parseArgument("blob", statement);
        assertEquals("blob", argument.objectPrefix);
        assertEquals(0, argument.objectNumber);
        assertEquals("/path/to/file", argument.destination);
    }

    @Test
    void error1() throws Exception {
        var statement = parse("blob");

        var e = assertThrows(TgsqlMessageException.class, () -> {
            target.parseArgument("blob", statement);
        });
        assertEquals("objectName not specified", e.getMessage());
    }

    @Test
    void error1_objectName() throws Exception {
        var statement = parse("blob@0");

        var e = assertThrows(TgsqlMessageException.class, () -> {
            target.parseArgument("blob", statement);
        });
        assertEquals("destination not specified", e.getMessage());
    }

    @Test
    void error1_objectName_short() throws Exception {
        var statement = parse("b@0");

        var e = assertThrows(TgsqlMessageException.class, () -> {
            target.parseArgument("blob", statement);
        });
        assertEquals("illegal objectName. target=blob, objectName=b@0", e.getMessage());
    }

    @Test
    void error2() throws Exception {
        var statement = parse("blob 0");

        var e = assertThrows(TgsqlMessageException.class, () -> {
            target.parseArgument("blob", statement);
        });
        assertEquals("destination not specified", e.getMessage());
    }

    @Test
    void error2_unmatch() throws Exception {
        var statement = parse("blob clob@0");

        var e = assertThrows(TgsqlMessageException.class, () -> {
            target.parseArgument("blob", statement);
        });
        assertEquals("illegal objectName. target=blob, objectName=clob@0", e.getMessage());
    }

    @Test
    void error2_unmatch2() throws Exception {
        var statement = parse("blob b@0");

        var e = assertThrows(TgsqlMessageException.class, () -> {
            target.parseArgument("blob", statement);
        });
        assertEquals("illegal objectName. target=blob, objectName=b@0", e.getMessage());
    }

    private static SpecialStatement parse(String s) throws IOException {
        String text = "\\store " + s;
        try (var parser = new SqlParser(new StringReader(text))) {
            return (SpecialStatement) parser.next();
        }
    }
}
