package com.tsurugidb.console.cli.jline;

import java.io.IOException;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Tsurugi SQL console JLine LineReader
 */
public final class JlLineReader {

    public static LineReader create() throws IOException {
        var terminal = createTerminal();

        var parser = new JlLineParser();
        parser.setEscapeChars(null);

        var reader = LineReaderBuilder.builder() //
                .appName("Tsurugi SQL console") //
                .terminal(terminal) //
                .parser(parser) //
                .build();
        return reader;
    }

    private static Terminal createTerminal() throws IOException {
        var terminal = TerminalBuilder.terminal();

        Attributes originalAttributes = terminal.getAttributes();
        Attributes attributes = new Attributes(originalAttributes);
        attributes.setControlChar(ControlChar.VINTR, 0); // disable Ctrl+C
        terminal.setAttributes(attributes);

        return terminal;
    }

    private JlLineReader() {
        throw new AssertionError();
    }
}
