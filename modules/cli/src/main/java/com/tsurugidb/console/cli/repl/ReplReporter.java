package com.tsurugidb.console.cli.repl;

import java.text.MessageFormat;
import java.util.List;

import javax.annotation.Nonnull;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import com.tsurugidb.console.core.executor.report.ScriptReporter;

/**
 * Tsurugi SQL console repl Reporter.
 */
public class ReplReporter extends ScriptReporter {

    private static final int RED = 0xc0_00_00;
    private static final int GREEN = 0x00_c0_00;
    private static final int YELLOW = 0xc0_c0_00;

    private final Terminal terminal;

    /**
     * Creates a new instance.
     *
     * @param terminal JLine Terminal
     */
    public ReplReporter(@Nonnull Terminal terminal) {
        this.terminal = terminal;
    }

    @Override
    public void info(String message) {
        println(message);
    }

    @Override
    public void implicit(String message) {
        println(message, YELLOW);
    }

    @Override
    public void succeed(String message) {
        println(message, GREEN);
    }

    @Override
    public void warn(String message) {
        println(message, RED);
    }

    @Override
    public void reportTransactionStatus(String message, boolean active) {
        var color = active ? GREEN : RED;
        println(message, color);
    }

    @Override
    public void reportHelp(List<String> list) {
        int color = 0xe0_e0_e0;
        for (var s : list) {
            println(s, color);
        }
    }

    /**
     * output ResultSet header.
     *
     * @param text header
     */
    public void reportResultSetHeader(String text) {
        printlnBold(text);
    }

    /**
     * output ResultSet row.
     *
     * @param text row
     */
    public void reportResultSetRow(String text) {
        println(text);
    }

    /**
     * output ResultSet size.
     *
     * @param size row size
     */
    public void reportResultSetSize(int size) {
        String message = MessageFormat.format("({0} rows)", size);
        reportResultSetSize(message);
    }

    protected void reportResultSetSize(String text) {
        succeed(text);
    }

    /**
     * @param message message
     * @param color   0xrrggbb
     */
    private void println(String message, int color) {
        String styledMessage = new AttributedStringBuilder() //
                .style(AttributedStyle.DEFAULT.foregroundRgb(color)) //
                .append(message) //
                .style(AttributedStyle.DEFAULT) //
                .toAnsi(terminal);
        println(styledMessage);
    }

    private void printlnBold(String message) {
        String styledMessage = new AttributedStringBuilder() //
                .style(AttributedStyle.DEFAULT.bold()) //
                .append(message) //
                .style(AttributedStyle.DEFAULT) //
                .toAnsi(terminal);
        println(styledMessage);
    }

    private void println(String message) {
        terminal.writer().println(message);
    }
}
