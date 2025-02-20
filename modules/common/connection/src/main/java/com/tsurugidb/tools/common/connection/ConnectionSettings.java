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

import java.net.URI;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Settings of Tsurugi connections.
 */
public class ConnectionSettings {

    /**
     * A builder of {@link ConnectionSettings}.
     */
    public static class Builder {

        private URI endpointUri;

        private String applicationName = null;

        private String sessionLabel = null;

        private List<CredentialProvider> credentialProviders = List.of();

        private Duration establishTimeout = DEFAULT_ESTABLISH_TIMEOUT;

        /**
         * Creates a new instance from this builder settings.
         * @return the created instance
         */
        public ConnectionSettings build() {
            return new ConnectionSettings(this);
        }

        /**
         * Sets the connection endpoint URI.
         * @param value the endpoint URI
         * @return this
         */
        public Builder withEndpointUri(@Nonnull URI value) {
            this.endpointUri = value;
            return this;
        }

        /**
         * Sets the client application name.
         * @param value the client application name, or {@code null} to clear it
         * @return this
         */
        public Builder withApplicationName(@Nullable String value) {
            this.applicationName = value;
            return this;
        }

        /**
         * Sets the session label.
         * @param value the session label, or {@code null} to clear it
         * @return this
         */
        public Builder withSessionLabel(@Nullable String value) {
            this.sessionLabel = value;
            return this;
        }

        /**
         * Sets the series of {@link CredentialProvider credential providers}.
         * <p>
         * The library will attempt each of these credentials in the order within the list,
         * when connecting to the database.
         * </p>
         * @param values the series of credential providers
         * @return this
         */
        public Builder withCredentialProviders(@Nonnull List<? extends CredentialProvider> values) {
            Objects.requireNonNull(values);
            this.credentialProviders = List.copyOf(values);
            return this;
        }

        /**
         * Sets the timeout duration while establishing a connection.
         *
         * <p>
         * If the duration is less than {@link ConnectionSettings#MINIMUM_TIMEOUT_DURATION
         * the minimum timeout duration} except just {@code 0}, it is automatically set to the minimum value.
         * </p>
         * <p>
         * Note that, if there are more than one {@link #getCredentialProviders() credentials},
         * the maximum timeout duration becomes multiplied by the number of them.
         * </p>
         * @param value the timeout duration, or {@code 0} or {@code null} to disable timeout
         * @return this
         * @throws IllegalArgumentException if the value is negative
         */
        public Builder withEstablishTimeout(@Nullable Duration value) {
            if (value == null || value.isZero()) {
                this.establishTimeout = null;
            } else if (value.isNegative()) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "timeout duration must not be negative: {0}",
                        value));
            } else if (value.compareTo(MINIMUM_TIMEOUT_DURATION) < 0) {
                this.establishTimeout = MINIMUM_TIMEOUT_DURATION;
            } else {
                this.establishTimeout = value;
            }
            return this;
        }
    }

    /**
     * The default value of timeout value until connection was established (default: timeout disabled).
     */
    public static final Duration DEFAULT_ESTABLISH_TIMEOUT = null;

    /**
     * The minimum timeout duration ({@code 1}ms).
     */
    public static final Duration MINIMUM_TIMEOUT_DURATION = Duration.ofMillis(1);

    private final @Nonnull URI endpointUri;

    private final @Nullable String applicationName;

    private final @Nullable String sessionLabel;

    private final @Nonnull List<CredentialProvider> credentialProviders;

    private final @Nullable Duration establishTimeout;

    /**
     * Creates a new instance with default settings.
     * @param endpointUri the connection endpoint URI
     * @see #newBuilder()
     */
    public ConnectionSettings(@Nonnull URI endpointUri) {
        this(new Builder().withEndpointUri(endpointUri));
    }

    /**
     * Creates a new instance from the builder.
     * @param builder the source builder
     * @see #newBuilder()
     */
    public ConnectionSettings(@Nonnull Builder builder) {
        Objects.requireNonNull(builder);
        if (builder.endpointUri == null) {
            throw new IllegalStateException("endpoint URI is not set");
        }
        this.endpointUri = builder.endpointUri;
        this.applicationName = builder.applicationName;
        this.sessionLabel = builder.sessionLabel;
        this.credentialProviders = builder.credentialProviders;
        this.establishTimeout = builder.establishTimeout;
    }

    /**
     * Creates a new builder object for this class.
     * @return the created builder object
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Returns the connection endpoint URI.
     * @return the endpoint URI
     */
    public URI getEndpointUri() {
        return endpointUri;
    }

    /**
     * Returns the client application name.
     * @return the application name, or {@code empty} if it is not set
     */
    public Optional<String> getApplicationName() {
        return Optional.ofNullable(applicationName);
    }

    /**
     * Returns the session label.
     * @return the session label, or {@code null} if it is not set
     */
    public Optional<String> getSessionLabel() {
        return Optional.ofNullable(sessionLabel);
    }

    /**
     * Returns the series of credential providers.
     * <p>
     * The library will attempt each of these credentials in the order within the list,
     * when connecting to the database.
     * </p>
     * @return the credential providers
     */
    public List<CredentialProvider> getCredentialProviders() {
        return credentialProviders;
    }

    /**
     * Returns the timeout duration while establishing a connection.
     *
     * <p>
     * If the duration is defined, it must be greater than or equal to {@link #MINIMUM_TIMEOUT_DURATION
     * the minimum timeout duration},
     * </p>
     * <p>
     * Note that, if there are more than one {@link #getCredentialProviders() credentials},
     * the maximum timeout duration becomes multiplied by the number of them.
     * </p>
     * @return the timeout duration, or {@code empty} if the timeout is disabled
     */
    public Optional<Duration> getEstablishTimeout() {
        return Optional.ofNullable(establishTimeout);
    }

    @Override
    public String toString() {
        return String.format(
                "ConnectionSettings(" //$NON-NLS-1$
                + "endpointUri=%s, " //$NON-NLS-1$
                + "applicationName=%s, sessionLabel=%s, " //$NON-NLS-1$
                + "credentialProviders=%s, " //$NON-NLS-1$
                + "establishTimeout=%s" //$NON-NLS-1$
                + ")", //$NON-NLS-1$
                endpointUri,
                applicationName, sessionLabel,
                credentialProviders,
                establishTimeout);
    }
}
