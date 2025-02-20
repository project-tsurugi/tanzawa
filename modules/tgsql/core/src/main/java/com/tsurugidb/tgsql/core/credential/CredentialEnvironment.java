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
package com.tsurugidb.tgsql.core.credential;

import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Environment variable for Tsurugi credential.
 */
public final class CredentialEnvironment {

    private static final String TSURUGI_HIDDEN_DIR = ".tsurugidb"; //$NON-NLS-1$

    /**
     * get the value of environment variable TSURUGI_AUTH_TOKEN.
     *
     * @return auth token
     */
    public static @Nonnull Optional<String> findTsurugiAuthToken() {
        return Optional.ofNullable(System.getenv("TSURUGI_AUTH_TOKEN")); //$NON-NLS-1$
    }

    /**
     * get path of credential.json under user.home .
     *
     * @return path
     */
    public static @Nonnull Optional<Path> findUserHomeCredentialPath() {
        return findUserHomePath("credentials.json"); //$NON-NLS-1$
    }

    /**
     * get path under user.home .
     *
     * @param relativePath relative path under user.home
     * @return path
     */
    public static Optional<Path> findUserHomePath(String relativePath) {
        return Optional.ofNullable(System.getProperty("user.home")) //$NON-NLS-1$
                .map(home -> Path.of(home, TSURUGI_HIDDEN_DIR, relativePath));
    }

    private CredentialEnvironment() {
        throw new AssertionError();
    }
}
