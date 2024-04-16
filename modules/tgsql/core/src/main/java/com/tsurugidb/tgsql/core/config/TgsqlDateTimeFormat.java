package com.tsurugidb.tgsql.core.config;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * dateTime format.
 */
public class TgsqlDateTimeFormat {

    /**
     * Creates a new instance.
     *
     * @param format format text
     * @return dateTime format
     */
    public static TgsqlDateTimeFormat create(String format) {
        if (format.isEmpty()) {
            return null;
        }

        var formatter = DateTimeFormatter.ofPattern(format);
        return new TgsqlDateTimeFormat(format, formatter);
    }

    private final String format;
    private final DateTimeFormatter formatter;

    /**
     * Creates a new instance.
     *
     * @param format    format text
     * @param formatter DateTimeFormatter
     */
    protected TgsqlDateTimeFormat(String format, DateTimeFormatter formatter) {
        this.format = format;
        this.formatter = formatter;
    }

    /**
     * get now.
     *
     * @return dateTime text
     */
    public String now() {
        return format(ZonedDateTime.now());
    }

    /**
     * get formatted text.
     *
     * @param dateTime dateTime
     * @return dateTime text
     */
    public String format(ZonedDateTime dateTime) {
        return dateTime.format(formatter);
    }

    @Override
    public String toString() {
        return this.format;
    }
}
