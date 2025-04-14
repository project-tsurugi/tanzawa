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

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.tgsql.core.model.ErroneousStatement;
import com.tsurugidb.tgsql.core.model.Region;
import com.tsurugidb.tgsql.core.model.Statement;

/**
 * Parses list of SQL statements and returns corresponding {@link Statement} objects.
 */
public class SqlParser implements Closeable {

    /**
     * Options of {@link SqlParser}.
     */
    public static class Options {

        /**
         * The default value of {@link #withSkipComments(boolean)} ({@value #DEFAULT_SKIP_COMMENTS}).
         */
        public static final boolean DEFAULT_SKIP_COMMENTS = SqlScanner.Options.DEFAULT_SKIP_COMMENTS;

        private final SqlScanner.Options scannerOpts = new SqlScanner.Options();

        /**
         * Sets whether to skip all comment tokens, including regular and documentation comments.
         * @param enabled {@code true} to skip comments, or {@code false} to treat comments as tokens
         * @return this
         * @see #withSkipRegularComments(boolean)
         * @see #withSkipDocumentationComments(boolean)
         * @see #DEFAULT_SKIP_COMMENTS
         */
        public Options withSkipComments(boolean enabled) {
            this.scannerOpts.skipRegularComments = enabled;
            this.scannerOpts.skipDocumentationComments = enabled;
            return this;
        }

        /**
         * Returns whether or not skip all comment tokens.
         * @return {@code true} to skip comments, or {@code false} to treat some comments as tokens
         * @see #isSkipRegularComments()
         * @see #isSkipDocumentationComments()
         * @see #DEFAULT_SKIP_COMMENTS
         */
        public boolean isSkipComments() {
            return isSkipRegularComments() && isSkipDocumentationComments();
        }

        /**
         * Sets whether to skip regular comments.
         * <p>
         * Even if this option is set to {@code true}, documentation comments are still effective.
         * </p>
         * @param enabled {@code true} to skip regular comments, or {@code false} to treat them as tokens
         * @return this
         * @see #withSkipDocumentationComments(boolean)
         */
        public Options withSkipRegularComments(boolean enabled) {
            this.scannerOpts.skipRegularComments = enabled;
            return this;
        }

        /**
         * Returns whether to skip regular comments.
         * @return {@code true} to skip regular comments, or {@code false} to treat them as tokens
         * @see #isSkipDocumentationComments()
         */
        public boolean isSkipRegularComments() {
            return this.scannerOpts.skipRegularComments;
        }

        /**
         * Sets whether to skip documentation comments ({@code /&#42;&#42; .. &#42;/}).
         * @param enabled {@code true} to skip documentation comments, or {@code false} to treat them as tokens
         * @return this
         * @see #withSkipRegularComments(boolean)
         */
        public Options withSkipDocumentationComments(boolean enabled) {
            this.scannerOpts.skipDocumentationComments = enabled;
            return this;
        }

        /**
         * Returns whether to skip documentation comments.
         * @return {@code true} to skip documentation comments, or {@code false} to treat them as tokens
         * @see #isSkipRegularComments()
         */
        public boolean isSkipDocumentationComments() {
            return this.scannerOpts.skipDocumentationComments;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((scannerOpts == null) ? 0 : scannerOpts.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Options other = (Options) obj;
            if (scannerOpts == null) {
                if (other.scannerOpts != null) {
                    return false;
                }
            } else if (!scannerOpts.equals(other.scannerOpts)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return String.format("{skipComments:%s}", //$NON-NLS-1$
                    scannerOpts.skipRegularComments);
        }
    }

    private final SqlScanner scanner;

    /**
     * Creates a new instance.
     * @param input the input source
     */
    public SqlParser(@Nonnull Reader input) {
        Objects.requireNonNull(input);
        this.scanner = new SqlScanner(input);
    }

    /**
     * Creates a new instance.
     * @param input the input source
     * @param options the parser options
     */
    public SqlParser(@Nonnull Reader input, @Nonnull Options options) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(options);
        this.scanner = new SqlScanner(input, options.scannerOpts);
    }

    /**
     * Consumes the next statement.
     * @return the next statement, or {@code null} after reached EOF
     * @throws IOException if I/O error was occurred
     */
    public Statement next() throws IOException {
        var segment = scanner.next();
        if (segment == null) {
            return null;
        }
        if (scanner.sawEof()
                && segment.getTokens().size() == 1
                && segment.getTokens().get(0).getKind() == TokenKind.END_OF_STATEMENT
                && segment.getComments().isEmpty()) {
            return null;
        }
        try {
            return SegmentAnalyzer.analyze(segment);
        } catch (ParseException e) {
            return new ErroneousStatement(
                    segment.getText(),
                    new Region(
                            segment.getOffset(),
                            segment.getText().length(),
                            segment.getStartLine(),
                            segment.getStartColumn()),
                    e.getErrorKind(),
                    e.getOccurrence(),
                    e.getLocalizedMessage());
        }
    }

    /**
     * Returns whether or not this scanner reached EOF.
     * @return {@code true} is reached EOF, otherwise {@code false}
     */
    public boolean sawEof() {
        return scanner.sawEof();
    }

    @Override
    public void close() throws IOException {
        scanner.close();
    }
}
