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
import java.util.concurrent.CancellationException;

/**
 * An program entry of {@link ConsoleCredentialPrompt} for testing.
 */
public class ConsoleCredentialPromptRunner {

    /**
     * Prompt the user for credentials.
     * @param args ignored
     * @throws IOException if I/O error was occurred
     * @throws InterruptedException if the operation was interrupted
     */
    public static void main(String... args) throws IOException, InterruptedException {
        var prompt = new ConsoleCredentialPrompt(System.console());
        var username = prompt.getUsername().orElseThrow(CancellationException::new);
        var password = prompt.getPassword().orElseThrow(CancellationException::new);
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
    }
}
