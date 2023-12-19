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
