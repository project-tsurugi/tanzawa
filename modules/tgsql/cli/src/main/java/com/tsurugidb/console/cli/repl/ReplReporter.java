package com.tsurugidb.console.cli.repl;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import com.tsurugidb.console.core.config.ScriptColor;
import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.config.ScriptCvKey.ScriptCvKeyColor;
import com.tsurugidb.console.core.executor.report.ScriptReporter;
import com.tsurugidb.tsubakuro.sql.SqlServiceException;

/**
 * Tsurugi SQL console repl Reporter.
 */
public class ReplReporter extends ScriptReporter {

    private final Terminal terminal;
    private final ScriptConfig config;

    /**
     * Creates a new instance.
     *
     * @param terminal JLine Terminal
     * @param config   SQL scripts configuration
     */
    public ReplReporter(@Nonnull Terminal terminal, @Nonnull ScriptConfig config) {
        this.terminal = Objects.requireNonNull(terminal);
        this.config = Objects.requireNonNull(config);
    }

    // for test
    ReplReporter() {
        this.terminal = null;
        this.config = null;
    }

    protected int red() {
        return color(ReplCvKey.CONSOLE_WARNING_COLOR, 0xc0_00_00);
    }

    protected int green() {
        return color(ReplCvKey.CONSOLE_SUCCEED_COLOR, 0x00_c0_00);
    }

    protected int yellow() {
        return color(ReplCvKey.CONSOLE_IMPLICIT_COLOR, 0xc0_c0_00);
    }

    protected int color(ScriptCvKeyColor key, int defaultColor) {
        ScriptColor color = config.getClientVariableMap().get(key);
        if (color == null) {
            return defaultColor;
        }
        return color.rgb();
    }

    @Override
    public void info(String message) {
        int color = color(ReplCvKey.CONSOLE_INFO_COLOR, -1);
        println(message, color);
    }

    @Override
    public void implicit(String message) {
        println(message, yellow());
    }

    @Override
    public void succeed(String message) {
        println(message, green());
    }

    @Override
    public void warn(String message) {
        println(message, red());
    }

    @Override
    protected void reportSessionStatus(String message, String endpoint, boolean active) {
        int color = active ? green() : red();
        println(message, color);
    }

    @Override
    protected void reportTransactionStatus(String message, boolean active) {
        int color = active ? green() : red();
        println(message, color);
    }

    @Override
    protected void reportTransactionException(String message, SqlServiceException exception) {
        int color = (exception == null) ? green() : red();
        println(message, color);
    }

    @Override
    public void reportHelp(List<String> list) {
        int color = color(ReplCvKey.CONSOLE_HELP_COLOR, 0xe0_e0_e0);
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
     * @param over {@code true} if over max lines
     */
    public void reportResultSetSize(int size, boolean over) {
        String message = getResultSetSizeMessage(size, over);
        reportResultSetSize(message);
    }

    /**
     * get ResultSet size message.
     *
     * @param size row size
     * @param over {@code true} if over max lines
     * @return message
     */
    protected String getResultSetSizeMessage(int size, boolean over) {
        if (over) {
            return MessageFormat.format("({0,choice,0#0 rows|1#1 row|1<{0} rows} over)", size);
        } else {
            return MessageFormat.format("({0,choice,0#0 rows|1#1 row|1<{0} rows})", size);
        }
    }

    protected void reportResultSetSize(String text) {
        succeed(text);
    }

    /**
     * print message.
     *
     * @param message message
     * @param color   0xrrggbb
     */
    private void println(String message, int color) {
        if (color < 0) {
            println(message);
            return;
        }
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
        var writer = terminal.writer();
        writer.println(message);
        writer.flush();
    }
}
