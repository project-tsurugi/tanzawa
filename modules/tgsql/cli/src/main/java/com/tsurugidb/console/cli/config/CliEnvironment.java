package com.tsurugidb.console.cli.config;

import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.tsurugidb.console.core.credential.CredentialEnvironment;

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
