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
