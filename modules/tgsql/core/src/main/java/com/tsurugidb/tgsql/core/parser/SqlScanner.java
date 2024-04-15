package com.tsurugidb.tgsql.core.parser;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * Scans SQL compilation unit and splits it into segments of individual statements.
 */
class SqlScanner implements Closeable {

    /**
     * Options of {@link SqlScanner}.
     */
    static class Options {

        static final boolean DEFAULT_SKIP_COMMENTS = false;

        boolean skipComments = DEFAULT_SKIP_COMMENTS;

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (skipComments ? 1231 : 1237);
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
            if (skipComments != other.skipComments) {
                return false;
            }
            return true;
        }
    }

    private final SqlScannerFlex flex;

    /**
     * Creates a new instance with default options.
     *
     * @param input the input
     */
    SqlScanner(@Nonnull Reader input) {
        this(input, new Options());
    }

    /**
     * Creates a new instance.
     *
     * @param input   the input
     * @param options the scanner options
     */
    SqlScanner(@Nonnull Reader input, @Nonnull Options options) {
        Objects.requireNonNull(input);
        Objects.requireNonNull(options);
        flex = new SqlScannerFlex(input, options.skipComments);
    }

    /**
     * Returns the next segment.
     *
     * @return the next segment, or null if there are no more segments
     * @throws IOException if I/O error was occurred
     */
    Segment next() throws IOException {
        if (sawEof()) {
            return null;
        }
        while (true) {
            var saw = flex.yylex();
            if (saw == SqlScannerFlex.SAW_EOF || saw == SqlScannerFlex.SAW_DELIMITER) {
                return flex.build();
            }
        }
    }

    /**
     * Returns whether or not this scanner reached EOF.
     *
     * @return {@code true} is reached EOF, otherwise {@code false}
     */
    boolean sawEof() {
        return flex.yyatEOF();
    }

    @Override
    public void close() throws IOException {
        flex.yyclose();
    }
}
