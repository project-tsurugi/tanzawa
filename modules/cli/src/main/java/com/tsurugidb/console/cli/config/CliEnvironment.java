package com.tsurugidb.console.cli.config;

import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Environment variable for Tsurugi SQL console cli.
 */
public final class CliEnvironment {

    private static final String TSURUGI_HIDDEN_DIR = ".tsurugidb"; //$NON-NLS-1$
    private static final String PROJECT_DIR = "console"; //$NON-NLS-1$

    /**
     * get the value of environment variable TSURUGI_AUTH_TOKEN.
     * 
     * @return auth token
     */
    @Nonnull
    public static Optional<String> findTsurugiAuthToken() {
        return Optional.ofNullable(System.getenv("TSURUGI_AUTH_TOKEN")); //$NON-NLS-1$
    }

    /**
     * get path of credential.json under user.home
     * 
     * @return path
     */
    @Nonnull
    public static Optional<Path> findUserHomeCredentialPath() {
        return findUserHomePath("credentials.json"); //$NON-NLS-1$
    }

    /**
     * get path of repl history file under user.home
     * 
     * @return path
     */
    @Nonnull
    public static Optional<Path> findUserHomeReplHistoryPath() {
        return findUserHomePath(PROJECT_DIR + "/repl.history.txt"); //$NON-NLS-1$
    }

    private static Optional<Path> findUserHomePath(String relativePath) {
        return Optional.ofNullable(System.getProperty("user.home")) //$NON-NLS-1$
                .map(home -> Path.of(home, TSURUGI_HIDDEN_DIR, relativePath)); // $NON-NLS-1$
    }

    private CliEnvironment() {
        throw new AssertionError();
    }
}
