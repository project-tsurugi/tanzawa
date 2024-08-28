/*
 * Copyright 2023-2024 Project Tsurugi.
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

import java.util.Optional;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;

/**
 * Provides Tsurugi connection {@link Credential credentials}.
 */
public interface CredentialProvider {

    /**
     * Returns the credential type information which this will provide.
     * @return the credential type information
     */
    String getType();

    /**
     * Retrieves credentials.
     * @return credentials, or {@code empty} if it was disabled
     * @throws DiagnosticException if error occurred while retrieving credentials information
     */
    Optional<? extends Credential> get() throws DiagnosticException;
}
