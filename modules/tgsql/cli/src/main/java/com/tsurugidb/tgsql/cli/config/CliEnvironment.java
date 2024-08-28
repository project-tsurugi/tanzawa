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
package com.tsurugidb.tgsql.cli.config;

import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.tsurugidb.tgsql.core.credential.CredentialEnvironment;

/**
 * Environment variable for Tsurugi SQL console cli.
 */
public final class CliEnvironment {

    private static final String PROJECT_DIR = "tgsql"; //$NON-NLS-1$

    /**
     * get path of console history file under user.home .
     *
     * @return path
     */
    public static @Nonnull Optional<Path> findUserHomeConsoleHistoryPath() {
        return CredentialEnvironment.findUserHomePath(PROJECT_DIR + "/console.history.txt"); //$NON-NLS-1$
    }

    /**
     * get path of client-variable.properties under user.home .
     *
     * @return path
     */
    public static @Nonnull Optional<Path> findUserHomeClientVariablePath() {
        return CredentialEnvironment.findUserHomePath(PROJECT_DIR + "/client-variable.properties"); //$NON-NLS-1$
    }

    private CliEnvironment() {
        throw new AssertionError();
    }
}
