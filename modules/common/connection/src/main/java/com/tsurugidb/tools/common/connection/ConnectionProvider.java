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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.exception.CoreServiceCode;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Provides connection to Tsurugi server.
 */
public class ConnectionProvider {

    static final Logger LOG = LoggerFactory.getLogger(ConnectionProvider.class);

    /**
     * Creates a new connection.
     * @param settings the connection settings
     * @return the created connection session
     * @throws InterruptedException if interrupted while connecting to the server
     * @throws DiagnosticException if error occurred while connecting to the Tsurugi server
     */
    public Session connect(@Nonnull ConnectionSettings settings) throws InterruptedException, DiagnosticException {
        Objects.requireNonNull(settings);
        LOG.trace("enter: connect: {}", settings); //$NON-NLS-1$
        LOG.debug("connecting to {}", settings.getEndpointUri()); //$NON-NLS-1$

        var attemptList = settings.getCredentialProviders();
        var attemptFailures = new ArrayList<ServerException>();
        for (int i = 0, n = attemptList.size(); i < n; i++) {
            var attempt = attemptList.get(i);

            LOG.debug("retrieving credential ({}/{}): {}", i + 1, attemptList.size(), attempt.getType()); //$NON-NLS-1$
            var credential = attempt.get();
            if (credential.isEmpty()) {
                LOG.debug("skipped disabled attempt: {}", attempt.getType()); //$NON-NLS-1$
                continue;
            }

            LOG.debug("trying attempt: {} ({})", settings.getEndpointUri(), attempt.getType()); //$NON-NLS-1$
            try {
                var result = attempt(settings, credential.get());
                LOG.trace("exit: connect: {}", settings); //$NON-NLS-1$
                return result;
            } catch (IOException e) {
                LOG.debug("exception was occurred in connection attempt", e); //$NON-NLS-1$
                throw new ConnectionException(
                        ConnectionDiagnosticCode.IO_ERROR,
                        List.of(DiagnosticUtil.getMessage(e)),
                        e);
            } catch (TimeoutException e) {
                LOG.debug("timeout in connection attempt", e); //$NON-NLS-1$
                throw new ConnectionException(
                        ConnectionDiagnosticCode.TIMEOUT,
                        List.of(settings.getEndpointUri(), settings.getEstablishTimeout().orElse(Duration.ZERO)),
                        e);
            } catch (ServerException e) {
                // escape authentication errors and try the next attempt
                // NOTE: NullCredential request also should raise "AUTHENTICATION_ERROR".
                if (e.getDiagnosticCode() == CoreServiceCode.AUTHENTICATION_ERROR
                        || e.getDiagnosticCode() == CoreServiceCode.INVALID_REQUEST) {
                    LOG.debug("authentication error in connection attempt", e); //$NON-NLS-1$
                    attemptFailures.add(e);
                    continue;
                }
                LOG.debug("exception was occurred in connection attempt", e); //$NON-NLS-1$
                throw new ConnectionException(
                        ConnectionDiagnosticCode.FAILURE,
                        List.of(settings.getEndpointUri(), DiagnosticUtil.getMessage(e)),
                        e);
            }
        }

        // if there are no authentication failures, we just raise diagnostic exception without any causes
        if (attemptFailures.isEmpty()) {
            throw new ConnectionException(
                    ConnectionDiagnosticCode.AUTHENTICATION_FAILURE,
                    List.of(settings.getEndpointUri()));
        }

        // or pick only the last failure and suppress the others.
        var error = new ConnectionException(
                ConnectionDiagnosticCode.AUTHENTICATION_FAILURE,
                List.of(settings.getEndpointUri()),
                attemptFailures.get(attemptFailures.size() - 1));
        for (int i = 0, n = attemptFailures.size() - 1; i < n; i++) {
            error.addSuppressed(attemptFailures.get(i));
        }
        throw error;
    }

    /**
     * Attempts to connect to the Tsurugi server.
     * @param settings the connection settings
     * @param credential the credential to attempt
     * @return the connection session
     * @throws InterruptedException if interrupted while connecting to the server
     * @throws IOException if I/O error was occurred while connecting to the server
     * @throws ServerException if the server returns an error during connection
     * @throws TimeoutException if the connection was timeout
     */
    protected Session attempt(@Nonnull ConnectionSettings settings, @Nonnull Credential credential)
            throws InterruptedException, IOException, ServerException, TimeoutException {
        Objects.requireNonNull(settings);
        Objects.requireNonNull(credential);
        LOG.trace("enter: attempt: {} ({})", credential, settings); //$NON-NLS-1$
        var builder = SessionBuilder.connect(settings.getEndpointUri());
        builder.withCredential(credential);
        settings.getApplicationName().ifPresent(builder::withApplicationName);
        settings.getSessionLabel().ifPresent(builder::withLabel);
        var timeout = settings.getEstablishTimeout();
        Session result;
        if (timeout.isEmpty()) {
            result = builder.create();
        } else {
            result = builder.create(timeout.get().toMillis(), TimeUnit.MILLISECONDS);
        }
        LOG.trace("exit: attempt: {} ({})", credential, settings); //$NON-NLS-1$
        return result;
    }
}
