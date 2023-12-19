package com.tsurugidb.tools.common.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.impl.SessionImpl;
import com.tsurugidb.tsubakuro.exception.CoreServiceCode;
import com.tsurugidb.tsubakuro.exception.CoreServiceException;

class ConnectionProviderTest {

    static final Session MOCK = new SessionImpl(new MockWire());

    static final CredentialProvider CP = new BasicCredentialProvider("testing", NullCredential.INSTANCE);

    @Test
    void connect() throws Exception {
        var provider = new ConnectionProvider() {
            @Override
            protected Session attempt(ConnectionSettings settings, Credential credential) {
                assertEquals(URI.create("ipc:testing"), settings.getEndpointUri());
                assertSame(NullCredential.INSTANCE, credential);
                return MOCK;
            }
        };
        var settings = ConnectionSettings.newBuilder()
                .withEndpointUri(URI.create("ipc:testing"))
                .withCredentialProviders(List.of(CP))
                .build();
        var result = provider.connect(settings);
        assertSame(MOCK, result);
    }

    @Test
    void connect_no_credentials() throws Exception {
        var provider = new ConnectionProvider() {
            @Override
            protected Session attempt(ConnectionSettings settings, Credential credential) {
                return MOCK;
            }
        };
        var settings = ConnectionSettings.newBuilder()
                .withEndpointUri(URI.create("ipc:testing"))
                .build();
        var e = assertThrows(DiagnosticException.class, () -> provider.connect(settings));
        assertEquals(ConnectionDiagnosticCode.AUTHENTICATION_FAILURE, e.getDiagnosticCode());
    }

    @Test
    void error_authentication_failure() throws Exception {
        var provider = new ConnectionProvider() {
            @Override
            protected Session attempt(ConnectionSettings settings, Credential credential) throws CoreServiceException {
                throw new CoreServiceException(CoreServiceCode.AUTHENTICATION_ERROR);
            }
        };
        var settings = ConnectionSettings.newBuilder()
                .withEndpointUri(URI.create("testing:testing"))
                .withCredentialProviders(List.of(CP))
                .build();
        var e = assertThrows(DiagnosticException.class, () -> provider.connect(settings));
        assertEquals(ConnectionDiagnosticCode.AUTHENTICATION_FAILURE, e.getDiagnosticCode());
    }

    @Test
    void connect_multiple_attempts() throws Exception {
        var provider = new ConnectionProvider() {
            @Override
            protected Session attempt(ConnectionSettings settings, Credential credential) throws CoreServiceException {
                if (((UsernamePasswordCredential) credential).getName().equals("OK")) {
                    return MOCK;
                }
                throw new CoreServiceException(CoreServiceCode.AUTHENTICATION_ERROR);
            }
        };
        var settings = ConnectionSettings.newBuilder()
                .withEndpointUri(URI.create("ipc:testing"))
                .withCredentialProviders(List.of(
                        new BasicCredentialProvider("a", new UsernamePasswordCredential("error", "p")),
                        new BasicCredentialProvider("b", new UsernamePasswordCredential("wrong", "p")),
                        new BasicCredentialProvider("c", new UsernamePasswordCredential("OK", "p"))))
                .build();
        var result = provider.connect(settings);
        assertSame(MOCK, result);
    }

    @Test
    void error_authentication_failure_multiple() throws Exception {
        var attempts = new AtomicInteger();
        var provider = new ConnectionProvider() {
            @Override
            protected Session attempt(ConnectionSettings settings, Credential credential) throws CoreServiceException {
                attempts.incrementAndGet();
                throw new CoreServiceException(CoreServiceCode.AUTHENTICATION_ERROR);
            }
        };
        var settings = ConnectionSettings.newBuilder()
                .withEndpointUri(URI.create("testing:testing"))
                .withCredentialProviders(List.of(
                        new BasicCredentialProvider("a", NullCredential.INSTANCE),
                        new BasicCredentialProvider("b", NullCredential.INSTANCE),
                        new BasicCredentialProvider("c", NullCredential.INSTANCE)))
                .build();
        var e = assertThrows(DiagnosticException.class, () -> provider.connect(settings));
        assertEquals(ConnectionDiagnosticCode.AUTHENTICATION_FAILURE, e.getDiagnosticCode());
        assertEquals(3, attempts.get());
    }

    @Test
    void error_connection_failure() throws Exception {
        var provider = new ConnectionProvider() {
            @Override
            protected Session attempt(ConnectionSettings settings, Credential credential) throws CoreServiceException {
                throw new CoreServiceException(CoreServiceCode.INVALID_REQUEST);
            }
        };
        var settings = ConnectionSettings.newBuilder()
                .withEndpointUri(URI.create("testing:testing"))
                .withCredentialProviders(List.of(CP))
                .build();
        var e = assertThrows(DiagnosticException.class, () -> provider.connect(settings));
        assertEquals(ConnectionDiagnosticCode.FAILURE, e.getDiagnosticCode());
    }

    @Test
    void error_connection_timeout() throws Exception {
        var provider = new ConnectionProvider() {
            @Override
            protected Session attempt(ConnectionSettings settings, Credential credential) throws TimeoutException {
                throw new TimeoutException();
            }
        };
        var settings = ConnectionSettings.newBuilder()
                .withEndpointUri(URI.create("testing:testing"))
                .withCredentialProviders(List.of(CP))
                .build();
        var e = assertThrows(DiagnosticException.class, () -> provider.connect(settings));
        assertEquals(ConnectionDiagnosticCode.TIMEOUT, e.getDiagnosticCode());
    }

    @Test
    void error_io() throws Exception {
        var provider = new ConnectionProvider() {
            @Override
            protected Session attempt(ConnectionSettings settings, Credential credential) throws IOException {
                throw new IOException();
            }
        };
        var settings = ConnectionSettings.newBuilder()
                .withEndpointUri(URI.create("testing:testing"))
                .withCredentialProviders(List.of(CP))
                .build();
        var e = assertThrows(DiagnosticException.class, () -> provider.connect(settings));
        assertEquals(ConnectionDiagnosticCode.IO_ERROR, e.getDiagnosticCode());
    }
}
