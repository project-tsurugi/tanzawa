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

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.model.Statement;
import com.tsurugidb.tgsql.core.parser.SqlParser;

/**
 * Tsurugi SQL console JLine Parser.
 */
public class ReplJLineParser extends DefaultParser {
    private static final Logger LOG = LoggerFactory.getLogger(ReplJLineParser.class);

    /**
     * Creates a new instance.
     */
    public ReplJLineParser() {
        setEscapeChars(null);
    }

    /**
     * parse.
     *
     * @param line    input text
     * @param cursor  cursor position
     * @param context parse context
     * @return parsed text
     */
    @Override
    public ParsedLine parse(String line, int cursor, ParseContext context) {
        if (context == ParseContext.ACCEPT_LINE) {
            String input = line + "\n"; //$NON-NLS-1$
            LOG.trace("input=[{}]", input); //$NON-NLS-1$
            var parserOptions = new SqlParser.Options().withSkipRegularComments(true);
            try (var reader = new StringReader(input); //
                    var parser = new SqlParser(reader, parserOptions)) {
                var statementList = new ArrayList<Statement>();
                while (true) {
                    var statement = parser.next();
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("statement={}, sawEof={}", statement, parser.sawEof()); //$NON-NLS-1$
                    }
                    if (statement == null) {
                        break;
                    }

                    if (parser.sawEof()) {
                        throw new EOFError(-1, -1, "end of statement"); //$NON-NLS-1$
                    }
                    statementList.add(statement);
                }
                var result = super.parse(line, cursor, context);
                return new ParsedStatement(result, statementList);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return super.parse(line, cursor, context);
    }

    /**
     * {@link ParsedLine} with {@link Statement}.
     */
    public static class ParsedStatement implements ParsedLine {
        private final ParsedLine delegate;
        private final List<Statement> statementList;

        ParsedStatement(@Nonnull ParsedLine delegate, @Nonnull List<Statement> statementList) {
            this.delegate = delegate;
            this.statementList = statementList;
        }

        @Override
        public String word() {
            return delegate.word();
        }

        @Override
        public int wordCursor() {
            return delegate.wordCursor();
        }

        @Override
        public int wordIndex() {
            return delegate.wordIndex();
        }

        @Override
        public List<String> words() {
            return delegate.words();
        }

        @Override
        public String line() {
            return delegate.line();
        }

        @Override
        public int cursor() {
            return delegate.cursor();
        }

        /**
         * The statement.
         *
         * @return statement
         */
        @Nonnull
        public List<Statement> statements() {
            return this.statementList;
        }
    }
}
