package com.tsurugidb.console.cli.repl.jline;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;

import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.parser.SqlParser;

/**
 * Tsurugi SQL console JLine Parser.
 */
public class ReplJLineParser extends DefaultParser {
    private static final Logger LOG = LoggerFactory.getLogger(ReplJLineParser.class);

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
            try (var reader = new StringReader(input); //
                    var parser = new SqlParser(reader)) {
                var statement = parser.next();
                if (LOG.isTraceEnabled()) {
                    LOG.trace("statement={}, sawEof={}", statement, parser.sawEof()); //$NON-NLS-1$
                }
                if (statement != null) {
                    if (parser.sawEof()) {
                        throw new EOFError(-1, -1, "end of statement"); //$NON-NLS-1$
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return super.parse(line, cursor, context);
    }
}
