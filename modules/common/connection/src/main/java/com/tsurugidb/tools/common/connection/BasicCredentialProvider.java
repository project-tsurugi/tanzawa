package com.tsurugidb.tools.common.connection;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;

/**
 * A basic implementation of {@link CredentialProvider} which provides pre-defined credentials.
 */
public class BasicCredentialProvider implements CredentialProvider {

    private final String type;

    private final Credential credential;

    /**
     * Creates a new instance.
     * @param type the type information
     * @param credential the credential to provide
     */
    public BasicCredentialProvider(@Nonnull String type, @Nonnull Credential credential) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(credential);
        this.type = type;
        this.credential = credential;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Optional<? extends Credential> get() throws DiagnosticException {
        return Optional.of(credential);
    }

    @Override
    public String toString() {
        return String.format("BasicCredentialProvider(type=%s, credential=%s)", type, credential); //$NON-NLS-1$
    }
}
