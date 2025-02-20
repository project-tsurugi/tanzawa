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
