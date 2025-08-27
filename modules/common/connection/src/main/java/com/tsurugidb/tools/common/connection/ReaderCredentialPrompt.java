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

import java.io.BufferedReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An implementation of {@link CredentialPrompt} that reads credentials from {@link Reader}.
 * <p>
 * This does not show prompt messages, and simply reads username and password from the provided {@link Reader}.
 * </p>
 */
public class ReaderCredentialPrompt implements CredentialPrompt {

    private final BufferedReader reader;

    private final Appendable writer;

    /**
     * Creates a new instance.
     * <p>
     * Note that, this never closes the given reader and writers.
     * </p>
     * @param reader the reader
     * @param writer an optional prompt string writer, that accepts {@link java.io.Writer}, {@link java.io.PrintStream}, and etc.
     */
    public ReaderCredentialPrompt(@Nonnull Reader reader, @Nullable Appendable writer) {
        Objects.requireNonNull(reader);
        this.reader = (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);
        this.writer = writer;
    }

    /**
     * Creates a new instance, without prompt strings.
     * <p>
     * Note that, this never closes the given {@link Reader}.
     * </p>
     * @param reader the reader
     */
    public ReaderCredentialPrompt(@Nonnull Reader reader) {
        this(reader, null);
    }

    @Override
    public Optional<String> getUsername() throws IOException, InterruptedException {
        try {
            write("username: ");
            return Optional.ofNullable(reader.readLine())
                    .map(String::trim)
                    .filter(s -> !s.isEmpty());
        } catch (IOException e) {
            throw rethrow(e);
        }
    }

    @Override
    public Optional<String> getPassword() throws IOException, InterruptedException {
        try {
            write("password: ");
            return Optional.ofNullable(reader.readLine());
        } catch (IOException e) {
            throw rethrow(e);
        }
    }

    private void write(String message) throws IOException {
        if (writer == null) {
            return;
        }
        writer.append(message);
        if (writer instanceof Flushable) {
            ((Flushable) writer).flush();
        }
    }

    private static IOException rethrow(IOException exception) throws InterruptedException {
        if (exception instanceof InterruptedIOException) {
            var cause = exception.getCause();
            if (cause instanceof InterruptedException) {
                throw (InterruptedException) cause;
            }
            throw (InterruptedException) new InterruptedException().initCause(exception);
        }
        return exception;
    }
}
