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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tsubakuro.channel.common.connection.FileCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;

/**
 * Provides instances of {@link CredentialProvider}.
 */
public class CredentialProviderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CredentialProviderFactory.class);

    /**
     * The authentication type for authentication tokens.
     */
    public static final String CREDENTIAL_TYPE_PASSWORD = "password-prompt";

    /**
     * The authentication type for user/password credentials.
     */
    public static final String CREDENTIAL_TYPE_USERNAME_PASSWORD = "user-prompt";

    /**
     * The authentication type for authentication tokens.
     */
    public static final String CREDENTIAL_TYPE_TOKEN = "token";

    /**
     * The authentication type for credential files.
     */
    public static final String CREDENTIAL_TYPE_FILE = "file";

    /**
     * The authentication type for guest (null) credentials.
     */
    public static final String CREDENTIAL_TYPE_NULL = "guest";

    /**
     * The environment variable name for the authentication token.
     * @see #getDefaultRememberMeCredentialProvider()
     */
    public static final String KEY_AUTH_TOKEN = "TSURUGI_AUTH_TOKEN"; //$NON-NLS-1$

    /**
     * The Java system property key for the user home directory.
     */
    public static final String KEY_HOME = "user.home"; //$NON-NLS-1$

    /**
     * The default credential file path (relative from the user home directory).
     * @see #getDefaultFileCredentialProvider()
     */
    public static final String DEFAULT_FILE_CREDENTIAL_PATH = ".tsurugidb/credentials.key"; //$NON-NLS-1$

    private static Reader standardInputReader;

    /**
     * Returns a new {@link CredentialProvider} that provides {@link NullCredential}.
     * @return the credential provider
     */
    public CredentialProvider getNullCredentialProvider() {
        return new BasicCredentialProvider(CREDENTIAL_TYPE_NULL, NullCredential.INSTANCE);
    }

    /**
     * Returns the default credential prompt from the standard input.
     * @return the default credential prompt
     */
    public CredentialPrompt getDefaultCredentialPrompt() {
        var console = System.console();
        if (console != null) {
            return new ConsoleCredentialPrompt(console);
        }
        return new ReaderCredentialPrompt(getStandardInputReader(), System.out);
    }

    private static synchronized Reader getStandardInputReader() {
        if (standardInputReader == null) {
            standardInputReader = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
        }
        return standardInputReader;
    }

    /**
     * Returns a new {@link CredentialProvider} that provides {@code UsernamePasswordCredential} from the prompt.
     * @param prompt the prompt controller
     * @param username the user name, or {@code null} if username should be also prompted
     * @return the credential provider
     * @throws IllegalArgumentException if username is empty
     * @see JLineCredentialPrompt
     * @see #getDefaultCredentialPrompt()
     */
    public CredentialProvider getPromptCredentialProvider(@Nonnull CredentialPrompt prompt, @Nullable String username) {
        Objects.requireNonNull(prompt);
        if (username != null && username.isEmpty()) {
            throw new IllegalArgumentException("username must not be empty"); //$NON-NLS-1$
        }
        var type = username == null ? CREDENTIAL_TYPE_USERNAME_PASSWORD : CREDENTIAL_TYPE_PASSWORD;
        return new PromptCredentialProvider(type, prompt, username);
    }

    /**
     * Returns a new {@link CredentialProvider} that provides {@link RememberMeCredential} from the given token.
     * @param token the authentication token
     * @return the credential provider
     * @throws IllegalArgumentException if authentication token is empty
     */
    public CredentialProvider getRememberMeCredentialProvider(@Nonnull String token) {
        Objects.requireNonNull(token);
        if (token.isEmpty()) {
            throw new IllegalArgumentException("authentication token must not be empty"); //$NON-NLS-1$
        }
        return new BasicCredentialProvider(CREDENTIAL_TYPE_TOKEN, new RememberMeCredential(token));
    }

    /**
     * Returns a {@link CredentialProvider} provides authentication token from the default environment variable,
     * only if it was declared.
     * @return the credential provider, or {@code empty} if the environment variable is not set
     * @see #KEY_AUTH_TOKEN
     */
    public Optional<CredentialProvider> getDefaultRememberMeCredentialProvider() {
        LOG.debug("retrieving default authentication token"); //$NON-NLS-1$
        return getDefaultAuthenticationToken()
                .map(token -> {
                    LOG.debug("found default authentication token"); //$NON-NLS-1$
                    return getRememberMeCredentialProvider(token);
                });
    }

    Optional<String> getDefaultAuthenticationToken() {
        return Optional.ofNullable(System.getenv(KEY_AUTH_TOKEN))
                .map(String::trim)
                .filter(s -> !s.isEmpty());
    }

    private static Optional<CredentialProvider> getFileCredentialProvider0(@Nonnull Path path) throws IOException {
        Objects.requireNonNull(path);
        LOG.debug("loading credential file: {}", path); //$NON-NLS-1$
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        return Optional.of(FileCredential.load(path))
                .map(it -> new BasicCredentialProvider(CREDENTIAL_TYPE_FILE, it));
    }

    /**
     * Returns a {@link FileCredential} that provides credential file from the specified file path.
     * @param path the file path for the credential
     * @return the credential provider
     * @throws DiagnosticException if error was occurred while loading the credential file
     */
    public CredentialProvider getFileCredentialProvider(@Nonnull Path path) throws DiagnosticException {
        Objects.requireNonNull(path);
        try {
            return getFileCredentialProvider0(path)
                    .orElseThrow(() -> new ConnectionException(
                    ConnectionDiagnosticCode.CREDENTIAL_ERROR,
                    List.of(
                            CREDENTIAL_TYPE_FILE,
                            MessageFormat.format("Credential file not found: {0}", path))));
        } catch (IOException e) {
            LOG.debug("exception was occurred in loading credential file: {}", path, e); //$NON-NLS-1$
            throw new ConnectionException(
                    ConnectionDiagnosticCode.CREDENTIAL_ERROR,
                    List.of(CREDENTIAL_TYPE_FILE, e.toString()));
        }
    }

    /**
     * Returns a {@link FileCredential} that provides credential file from the default file path, only if it exists.
     * @return the credential provider, or {@code empty} if the credential file does not exist
     * @throws IOException if I/O error was occurred while reading the credential file
     * @see #DEFAULT_FILE_CREDENTIAL_PATH
     */
    public Optional<CredentialProvider> getDefaultFileCredentialProvider() throws IOException {
        LOG.debug("retrieving default credential file"); //$NON-NLS-1$
        var path = getUserHomePath()
                .map(it -> it.resolve(DEFAULT_FILE_CREDENTIAL_PATH));
        if (path.isPresent()) {
            return getFileCredentialProvider0(path.get())
                    .map(it -> {
                        LOG.debug("found default credential file: {}", path.get()); //$NON-NLS-1$
                        return it;
                    });
        }
        return Optional.empty();
    }

    Optional<Path> getUserHomePath() {
        return Optional.ofNullable(System.getProperty(KEY_HOME))
                .map(String::trim)
                .filter(it -> !it.isEmpty())
                .map(Path::of);
    }

    /**
     * Returns the series of credential providers for default authentication operations.
     * <p>
     * Note: this will suppress I/O errors while reading the credential files, and then report them as warning logs.
     * </p>
     * @param prompt the credential prompt, or {@code null} to omit username/password prompt
     * @return the default credential providers
     * @see JLineCredentialPrompt
     * @see #getDefaultCredentialPrompt()
     */
    public List<CredentialProvider> getDefaultCredentialProviders(@Nullable CredentialPrompt prompt) {
        var results = new ArrayList<CredentialProvider>();

        // auth token
        getDefaultRememberMeCredentialProvider().ifPresent(results::add);

        // file credential
        try {
            getDefaultFileCredentialProvider().ifPresent(results::add);
        } catch (IOException e) {
            LOG.warn("error occurred while loading the default credential file", e);
        }

        // guest
        results.add(new BasicCredentialProvider(CREDENTIAL_TYPE_NULL, NullCredential.INSTANCE));

        // password
        if (prompt != null) {
            results.add(getPromptCredentialProvider(prompt, null));
        }

        return results;
    }
}
