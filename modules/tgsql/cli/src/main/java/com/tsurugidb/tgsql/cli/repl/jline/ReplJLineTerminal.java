package com.tsurugidb.tgsql.cli.repl.jline;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.OSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tsurugi SQL console JLine Terminal.
 */
public final class ReplJLineTerminal {
    private static final Logger LOG = LoggerFactory.getLogger(ReplJLineTerminal.class);

    private static Terminal staticTerminal;
    private static Attributes originalAttributes;

    /**
     * get terminal.
     *
     * @return terminal
     */
    public static Terminal getTerminal() {
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
            LOG.debug("terminal.class={}", terminal.getClass().getName()); //$NON-NLS-1$
            LOG.debug("IS_WINDOWS=" + OSUtils.IS_WINDOWS //$NON-NLS-1$
                    + ", IS_CYGWIN=" + OSUtils.IS_CYGWIN //$NON-NLS-1$
                    + ", IS_MSYSTEM=" + OSUtils.IS_MSYSTEM //$NON-NLS-1$
                    + ", IS_CONEMU=" + OSUtils.IS_CONEMU //$NON-NLS-1$
                    + ", IS_OSX=" + OSUtils.IS_OSX);
        }

        if (OSUtils.IS_CYGWIN || OSUtils.IS_MSYSTEM) {
            LOG.debug("disable VINTR");
            originalAttributes = terminal.getAttributes();
            Attributes attributes = new Attributes(originalAttributes);
            attributes.setControlChar(ControlChar.VINTR, 0); // disable Ctrl+C
            terminal.setAttributes(attributes);
        }

        return terminal;
    }

    /**
     * close terminal.
     *
     * @throws IOException if an I/O error occurs
     */
    public static void close() throws IOException {
        if (staticTerminal != null) {
            try {
                if (originalAttributes != null) {
                    staticTerminal.setAttributes(originalAttributes);
                    originalAttributes = null;
                }
            } finally {
                staticTerminal.close();
                staticTerminal = null;
            }
        }
    }

    private ReplJLineTerminal() {
        throw new AssertionError();
    }
}
