package com.tsurugidb.console.cli.jline;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;

import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.impl.DefaultParser;

import com.tsurugidb.console.core.parser.SqlParser;

/**
 * Tsurugi SQL console JLine Parser
 */
public class JlLineParser extends DefaultParser {

    @Override
    public ParsedLine parse(String line, int cursor, ParseContext context) {
        if (context == ParseContext.ACCEPT_LINE) {
            try (var reader = new StringReader(line); //
                    var parser = new SqlParser(reader)) {
                var statement = parser.next();
                if (statement != null) {
                    if (parser.sawEof()) {
                        throw new EOFError(-1, -1, "end of statement");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return super.parse(line, cursor, context);
    }
}
