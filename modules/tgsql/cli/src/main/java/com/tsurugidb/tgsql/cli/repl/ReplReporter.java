/*
 * Copyright 2023-2024 Project Tsurugi.
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
package com.tsurugidb.tgsql.cli.repl;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import com.tsurugidb.tgsql.core.config.TgsqlColor;
import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.config.TgsqlCvKey.TgsqlCvKeyColor;
import com.tsurugidb.tgsql.core.executor.report.TgsqlReporter;
import com.tsurugidb.tsubakuro.sql.SqlServiceException;

/**
 * Tsurugi SQL console repl Reporter.
 */
public class ReplReporter extends TgsqlReporter {

    private final Terminal terminal;

    /**
     * Creates a new instance.
     *
     * @param terminal JLine Terminal
     * @param config   tgsql configuration
     */
    public ReplReporter(@Nonnull Terminal terminal, @Nonnull TgsqlConfig config) {
        super(config);
        this.terminal = Objects.requireNonNull(terminal);
    }

    // for test
    ReplReporter() {
        super(new TgsqlConfig());
        this.terminal = null;
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

    protected int color(TgsqlCvKeyColor key, int defaultColor) {
        TgsqlColor color = config.getClientVariableMap().get(key);
        if (color == null) {
            return defaultColor;
        }
        return color.rgb();
    }

    @Override
    protected void doInfo(String message) {
        int color = color(ReplCvKey.CONSOLE_INFO_COLOR, -1);
        println(message, color);
    }

    @Override
    protected void doImplicit(String message) {
        println(message, yellow());
    }

    @Override
    protected void doSucceed(String message) {
        println(message, green());
    }

    @Override
    protected void doWarn(String message) {
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
