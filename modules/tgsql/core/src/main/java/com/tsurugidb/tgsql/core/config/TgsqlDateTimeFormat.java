/*
 * Copyright 2023-2025 Project Tsurugi.
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
