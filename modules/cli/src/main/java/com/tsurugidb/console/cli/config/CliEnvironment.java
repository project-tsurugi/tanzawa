package com.tsurugidb.console.cli.config;

import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.tsurugidb.console.core.credential.CredentialEnvironment;

/**
 * Environment variable for Tsurugi SQL console cli.
 */
public final class CliEnvironment {

    private static final String PROJECT_DIR = "console"; //$NON-NLS-1$

    /**
     * get path of repl history file under user.home
     *
     * @return path
     */
    public static @Nonnull Optional<Path> findUserHomeReplHistoryPath() {
        return CredentialEnvironment.findUserHomePath(PROJECT_DIR + "/repl.history.txt"); //$NON-NLS-1$
    }

    private CliEnvironment() {
        throw new AssertionError();
    }
}
