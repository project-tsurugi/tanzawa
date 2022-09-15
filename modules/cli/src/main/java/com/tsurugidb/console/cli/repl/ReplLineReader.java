package com.tsurugidb.console.cli.repl;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.utils.OSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

/**
 * Tsurugi SQL console JLine LineReader
 */
public final class ReplLineReader {
    private static final Logger LOG = LoggerFactory.getLogger(ReplLineReader.class);

    private static final String APP_NAME = "Tsurugi SQL console";

    private static Terminal staticTerminal;

    public static LineReader create() {
        var terminal = getTerminal();

        var parser = new ReplLineParser();
        parser.setEscapeChars(null);

        var reader = LineReaderBuilder.builder() //
                .appName(APP_NAME) //
                .terminal(terminal) //
                .parser(parser) //
                .build();
        return reader;
    }

    public static LineReader createSimpleReader() {
        var terminal = getTerminal();

        var reader = LineReaderBuilder.builder() //
                .appName(APP_NAME) //
                .terminal(terminal) //
                .build();
        return reader;
    }

    private static Terminal getTerminal() {
        if (staticTerminal == null) {
            try {
                staticTerminal = createTerminal();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return staticTerminal;
    }

    private static Terminal createTerminal() throws IOException {
        var terminal = TerminalBuilder.terminal();
        if (LOG.isDebugEnabled()) {
            LOG.debug("terminal.class={}", terminal.getClass().getName());
            LOG.debug("IS_WINDOWS=" + OSUtils.IS_WINDOWS //
                    + ", IS_CYGWIN=" + OSUtils.IS_CYGWIN //
                    + ", IS_MSYSTEM=" + OSUtils.IS_MSYSTEM //
                    + ", IS_CONEMU=" + OSUtils.IS_CONEMU //
                    + ", IS_OSX=" + OSUtils.IS_OSX //
            );
        }

        if (OSUtils.IS_CYGWIN || OSUtils.IS_MSYSTEM) {
            LOG.debug("disable VINTR");
            Attributes originalAttributes = terminal.getAttributes();
            Attributes attributes = new Attributes(originalAttributes);
            attributes.setControlChar(ControlChar.VINTR, 0); // disable Ctrl+C
            terminal.setAttributes(attributes);
        }

        return terminal;
    }

    private ReplLineReader() {
        throw new AssertionError();
    }
}
