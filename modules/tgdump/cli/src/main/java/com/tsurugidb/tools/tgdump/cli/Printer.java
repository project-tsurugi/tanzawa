package com.tsurugidb.tools.tgdump.cli;

import javax.annotation.Nonnull;

/**
 * Prints messages.
 */
@FunctionalInterface
public interface Printer {

    /**
     * Prints a message record onto the underlying device.
     * @param message the message
     */
    void print(@Nonnull String message);

    /**
     * Formats message (by {@link String#format(String, Object...)}) and print it as a record to underlying device.
     * @param format the message format
     * @param arguments the message arguments
     */
    default void printf(@Nonnull String format, @Nonnull Object... arguments) {
        print(String.format(format, arguments));
    }
}
