package com.tsurugidb.tools.tgdump.cli;

import java.io.PrintStream;
import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * A {@link Printer} to print to {@link PrintStream}.
 */
public class PrintStreamPrinter implements Printer {

    private final PrintStream output;

    /**
     * Creates a new instance.
     * @param output the destination
     */
    public PrintStreamPrinter(@Nonnull PrintStream output) {
        Objects.requireNonNull(output);
        this.output = output;
    }

    @Override
    public void print(@Nonnull String message) {
        output.println(message);
    }
}
