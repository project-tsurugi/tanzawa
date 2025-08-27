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

import java.io.IOError;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of {@link CredentialPrompt} that JLine.
 * <p>
 * This never close the underlying line reader.
 * </p>
 * <p>
 * <strong>Note:</strong> In JLine3, interruptions by Ctrl+C and by {@link Thread#interrupt()} are not distinguished.
 * Then in this implementation, both are treated as input cancellation, and {@link InterruptedException} is not thrown.
 * </p>
 */
public class JLineCredentialPrompt implements CredentialPrompt {

    private static final Logger LOG = LoggerFactory.getLogger(JLineCredentialPrompt.class);

    private final LineReader lineReader;

    /**
     * Creates a new instance.
     * @param reader the source line reader
     */
    public JLineCredentialPrompt(@Nonnull LineReader reader) {
        Objects.requireNonNull(reader);
        this.lineReader = reader;
    }

    /**
     * Returns the line reader.
     * @return the line reader
     */
    public LineReader getLineReader() {
        return lineReader;
    }

    @Override
    public Optional<String> getUsername() throws IOException {
        try {
            var line = lineReader.readLine("username: ");
            return Optional.of(line)
                    .map(String::trim)
                    .filter(it -> !it.isEmpty());
        } catch (UserInterruptException e) {
            LOG.debug("username input was canceled", e); //$NON-NLS-1$
            return Optional.empty();
        } catch (IOError e) {
            throw rethrow(e);
        }
    }

    @Override
    public Optional<String> getPassword() throws IOException {
        try {
            var line = lineReader.readLine("password: ", '*');
            return Optional.of(line);
        } catch (UserInterruptException e) {
            LOG.debug("password input was canceled", e); //$NON-NLS-1$
            return Optional.empty();
        } catch (IOError e) {
            throw rethrow(e);
        }
    }

    private static IOError rethrow(IOError e) throws IOException {
        var cause = e.getCause();
        if (cause instanceof IOException) {
            throw (IOException) cause;
        }
        return e;
    }
}
