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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

/**
 * An implementation of {@link CredentialProvider} that provides {@link UsernamePasswordCredential} from user input.
 */
public class PromptCredentialProvider implements CredentialProvider {

    private static final Logger LOG = LoggerFactory.getLogger(PromptCredentialProvider.class);

    private final String type;

    private final CredentialPrompt prompt;

    private final @Nullable String username;

    /**
     * Creates a new instance.
     * @param type the provider type name
     * @param prompt the console prompt
     * @param username the username, or {@code null} if also prompting for username
     * @throws IllegalArgumentException if the username is non-null but it was empty
     */
    public PromptCredentialProvider(@Nonnull String type, @Nonnull CredentialPrompt prompt, @Nullable String username) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(prompt);
        if (username != null && username.isEmpty()) {
            throw new IllegalArgumentException("username must not be empty");
        }
        this.type = type;
        this.prompt = prompt;
        this.username = username;
    }

    /**
     * Creates a new instance without user name.
     * @param type the provider type name
     * @param prompt the console prompt
     */
    public PromptCredentialProvider(@Nonnull String type, @Nonnull CredentialPrompt prompt) {
        this(type, prompt, null);
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public Optional<UsernamePasswordCredential> get() throws InterruptedException, DiagnosticException {
        try {
            String user = username;
            if (user == null) {
                LOG.debug("prompting for username"); //$NON-NLS-1$
                user = prompt.getUsername().orElse(null);
                if (user == null) {
                    LOG.debug("prompting for username was canceled"); //$NON-NLS-1$
                    return Optional.empty();
                }
                LOG.debug("prompting for username was successful: {}", user); //$NON-NLS-1$
            }
            LOG.debug("prompting for password"); //$NON-NLS-1$
            String password = prompt.getPassword().orElse(null);
            if (password == null) {
                LOG.debug("prompting for password was canceled"); //$NON-NLS-1$
                return Optional.empty();
            }
            LOG.debug("prompting for password was successful"); //$NON-NLS-1$
            return Optional.of(new UsernamePasswordCredential(user, password));
        } catch (IOException e) {
            LOG.debug("exception was occurred in prompting credential", e); //$NON-NLS-1$
            throw new ConnectionException(
                    ConnectionDiagnosticCode.CREDENTIAL_ERROR, List.of(getType(), e.toString()), e);
        }
    }

    @Override
    public String toString() {
        if (username == null) {
            return String.format("PromptCredentialProvider(type=%s)", type); //$NON-NLS-1$
        }
        return String.format("PromptCredentialProvider(type=%s, username=%s)", type, username); //$NON-NLS-1$
    }
}
