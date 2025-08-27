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

import java.io.IOException;
import java.util.Optional;

/**
 * A prompt interface to get username and password from users.
 */
public interface CredentialPrompt {

    /**
     * Obtains the username.
     * @return the non-empty username string, or {@code empty} if the prompt was canceled
     * @throws IOException if I/O error was occurred while reading the input
     * @throws InterruptedException if the operation was interrupted
     */
    default Optional<String> getUsername() throws IOException, InterruptedException {
        return Optional.empty();
    }

    /**
     * Obtains the password.
     * @return the password string, or {@code empty} if the prompt was canceled
     * @throws IOException if I/O error was occurred while reading the input
     * @throws InterruptedException if the operation was interrupted
     */
    default Optional<String> getPassword() throws IOException, InterruptedException {
        return Optional.empty();
    }
}
