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
package com.tsurugidb.tgsql.cli.repl.jline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.List;

import org.jline.reader.EOFError;
import org.jline.reader.Parser.ParseContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.tsurugidb.tgsql.cli.repl.jline.ReplJLineParser.ParsedStatement;
import com.tsurugidb.tgsql.core.model.Statement;
import com.tsurugidb.tgsql.core.model.Statement.Kind;

class ReplJLineParserTest {

    @Test
    void empty() {
        var statementList = parse("");
        assertEquals(0, statementList.size());
    }

    @Test
    void empty1() {
        var statementList = parse(";");
        assertEquals(1, statementList.size());
        assertEmpty(statementList.get(0));
    }

    @Test
    void empty2() {
        var statementList = parse(";;");
        assertEquals(2, statementList.size());
        assertEmpty(statementList.get(0));
        assertEmpty(statementList.get(1));
    }

    @Test
    void special1() {
        var statementList = parse("\\help");
        assertEquals(1, statementList.size());
        assertSpecial("\\help", statementList.get(0));
    }

    @Test
    void special1Semicolon() {
        var statementList = parse("\\help;");
        assertEquals(1, statementList.size());
        assertSpecial("\\help", statementList.get(0));
    }

    @Test
    void specialSymbol() {
        var statementList = parse("\\?");
        assertEquals(1, statementList.size());
        assertSpecial("\\?", statementList.get(0));
    }

    @Test
    void special0() {
        var statementList = parse("\\\n;");
        assertEquals(1, statementList.size());
        assertSql("\\\n", statementList.get(0));
    }

    @Test
    void special0Eof() {
        assertThrowsExactly(EOFError.class, () -> {
            parse("\\");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void special2(String separator) {
        var statementList = parse("\\help abc;" + separator + "\\help def");
        assertEquals(2, statementList.size());
        assertSpecial("\\help abc", statementList.get(0));
        assertSpecial("\\help def", statementList.get(1));
    }

    @Test
    void sqlEof() {
        assertThrowsExactly(EOFError.class, () -> {
            parse("select * from test");
        });
    }

    @Test
    void sql1() {
        var statementList = parse("select * from test;");
        assertEquals(1, statementList.size());
        assertSql("select * from test", statementList.get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void sql2(String separator) {
        var statementList = parse("select * from test1;" + separator + "select * from test2;");
        assertEquals(2, statementList.size());
        assertSql("select * from test1", statementList.get(0));
        assertSql("select * from test2", statementList.get(1));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void mix1(String separator) {
        var statementList = parse("\\help;" + separator + "select * from test;");
        assertEquals(2, statementList.size());
        assertSpecial("\\help", statementList.get(0));
        assertSql("select * from test", statementList.get(1));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void mix2(String separator) {
        var statementList = parse("select * from test;" + separator + "\\help");
        assertEquals(2, statementList.size());
        assertSql("select * from test", statementList.get(0));
        assertSpecial("\\help", statementList.get(1));
    }

    private static final ReplJLineParser PARSER = new ReplJLineParser();

    private static List<Statement> parse(String text) {
        var actual = (ParsedStatement) PARSER.parse(text, 0, ParseContext.ACCEPT_LINE);
        return actual.statements();
    }

    private static void assertEmpty(Statement actual) {
        assertEquals(Kind.EMPTY, actual.getKind());
    }

    private static void assertSpecial(String expectedText, Statement actual) {
        assertEquals(Kind.SPECIAL, actual.getKind());
        assertEquals(expectedText, actual.getText());
    }

    private static void assertSql(String expectedText, Statement actual) {
        assertEquals(Kind.GENERIC, actual.getKind());
        assertEquals(expectedText, actual.getText());
    }
}
