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
package com.tsurugidb.tgsql.core.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tgsql.core.model.Statement;

class SqlParserTest {

    @Test
    void simple() throws Exception {
        var ss = parse("SELECT * FROM T");
        assertEquals(1, ss.size());
        assertEquals(Statement.Kind.GENERIC, ss.get(0).getKind());
        assertEquals("SELECT * FROM T", ss.get(0).getText());
    }

    @Test
    void multiple() throws Exception {
        var ss = parse("SELECT * FROM T0; SELECT * FROM T1; SELECT * FROM T2");
        assertEquals(3, ss.size());

        assertEquals(Statement.Kind.GENERIC, ss.get(0).getKind());
        assertEquals("SELECT * FROM T0", ss.get(0).getText());

        assertEquals(Statement.Kind.GENERIC, ss.get(1).getKind());
        assertEquals("SELECT * FROM T1", ss.get(1).getText());

        assertEquals(Statement.Kind.GENERIC, ss.get(2).getKind());
        assertEquals("SELECT * FROM T2", ss.get(2).getText());
    }

    @Test
    void error() throws Exception {
        var ss = parse("ROLLBACK FROM T");
        assertEquals(1, ss.size());
        assertEquals(Statement.Kind.ERRONEOUS, ss.get(0).getKind());
    }

    @Test
    void empty_input() throws Exception {
        var ss = parse("");
        assertEquals(0, ss.size());
    }

    @Test
    void empty_statement() throws Exception {
        var ss = parse(";");
        assertEquals(1, ss.size());
        assertEquals(Statement.Kind.EMPTY, ss.get(0).getKind());
    }

    @Test
    void comments() throws Exception {
        var ss = parse("-- comment\nSELECT * FROM T");
        assertEquals(1, ss.size());
        assertEquals(Statement.Kind.GENERIC, ss.get(0).getKind());
        assertEquals("-- comment\nSELECT * FROM T", ss.get(0).getText());
    }

    @Test
    void comments_skip() throws Exception {
        var opts = new SqlParser.Options()
                .withSkipComments(true);
        var ss = parse(opts, "-- comment\nSELECT * FROM T");
        assertEquals(1, ss.size());
        assertEquals(Statement.Kind.GENERIC, ss.get(0).getKind());
        assertEquals("SELECT * FROM T", ss.get(0).getText());
    }

    private static List<Statement> parse(String text) throws IOException {
        try (var parser = new SqlParser(new StringReader(text))) {
            return parse0(parser);
        }
    }

    private static List<Statement> parse(SqlParser.Options opts, String text) throws IOException {
        try (var parser = new SqlParser(new StringReader(text), opts)) {
            return parse0(parser);
        }
    }

    private static List<Statement> parse0(SqlParser parser) throws IOException {
        List<Statement> results = new ArrayList<>();
        while (true) {
            var s = parser.next();
            if (s == null) {
                break;
            }
            results.add(s);
        }
        return results;
    }
}
