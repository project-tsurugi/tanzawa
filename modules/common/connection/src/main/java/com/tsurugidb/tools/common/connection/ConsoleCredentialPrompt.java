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
package com.tsurugidb.tools.common.connection;

import java.io.Console;
import java.io.IOError;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * An implementation of {@link CredentialPrompt} that uses the console for input.
 */
public class ConsoleCredentialPrompt implements CredentialPrompt {

    private final Console console;

    /**
     * Creates a new instance.
     * @param console the source console
     */
    public ConsoleCredentialPrompt(@Nonnull Console console) {
        Objects.requireNonNull(console);
        this.console = console;
    }

    @Override
    public Optional<String> getUsername() throws IOException, InterruptedException {
        try {
            return Optional.ofNullable(console.readLine("username: "))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty());
        } catch (IOError e) {
            throw rethrow(e);
        }
    }

    @Override
    public Optional<String> getPassword() throws IOException, InterruptedException {
        try {
            return Optional.ofNullable(console.readPassword("password: "))
                    .map(String::valueOf)
                    .map(String::trim);
        } catch (IOError e) {
            throw rethrow(e);
        }
    }

    private static IOError rethrow(IOError e) throws IOException, InterruptedException {
        var cause = e.getCause();
        if (cause instanceof InterruptedIOException) {
            var parent = cause.getCause();
            if (parent instanceof InterruptedException) {
                throw (InterruptedException) parent;
            }
            throw (InterruptedException) new InterruptedException().initCause(cause);
        }
        if (cause instanceof IOException) {
            throw (IOException) cause;
        }
        return e;
    }
}
