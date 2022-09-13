package com.tsurugidb.console.cli.repl;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Tsurugi SQL console JLine LineReader
 */
public final class ReplLineReader {

    private static Terminal TERMINAL;

    public static LineReader create() {
        var terminal = getTerminal();

        var parser = new ReplLineParser();
        parser.setEscapeChars(null);

        var reader = LineReaderBuilder.builder() //
                .appName("Tsurugi SQL console") //
                .terminal(terminal) //
                .parser(parser) //
                .build();
        return reader;
    }

    public static LineReader createSimpleReader() {
        var reader = LineReaderBuilder.builder() //
                .appName("Tsurugi SQL console") //
                .terminal(getTerminal()) //
                .build();
        return reader;
    }

    private static Terminal getTerminal() {
        if (TERMINAL == null) {
            try {
                TERMINAL = createTerminal();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return TERMINAL;
    }

    private static Terminal createTerminal() throws IOException {
        var terminal = TerminalBuilder.terminal();

        Attributes originalAttributes = terminal.getAttributes();
        Attributes attributes = new Attributes(originalAttributes);
        attributes.setControlChar(ControlChar.VINTR, 0); // disable Ctrl+C
        terminal.setAttributes(attributes);

        return terminal;
    }

    private ReplLineReader() {
        throw new AssertionError();
    }
}
