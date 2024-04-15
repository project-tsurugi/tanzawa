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
