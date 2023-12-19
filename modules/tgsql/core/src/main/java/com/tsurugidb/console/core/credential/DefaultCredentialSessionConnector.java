package com.tsurugidb.console.core.credential;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.FileCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.exception.CoreServiceCode;
import com.tsurugidb.tsubakuro.exception.CoreServiceException;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Session connector with default credential.
 */
public class DefaultCredentialSessionConnector {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCredentialSessionConnector.class);

    /**
     * Session with Credential
     */
    public static /* record */ class SessionWithCredential {
        private Session session;
        private Credential credential;

        SessionWithCredential(Session session, Credential credential) {
            this.session = session;
            this.credential = credential;
        }

        /**
         * get session.
         *
         * @return session
         */
        public Session session() {
            return this.session;
        }

        /**
         * get credential.
         *
         * @return credential
         */
        public Credential credential() {
            return this.credential;
        }
    }

    @FunctionalInterface
    private interface CredentialGetter {
        Credential get() throws IOException;
    }

    /**
     * connect.
     *
     * @param endpoint the end-point URI string
     * @return the established connection session
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws ServerException      if server side error was occurred
     * @throws InterruptedException if interrupted while executing the statement
     */
    public SessionWithCredential connect(@Nonnull String endpoint) throws IOException, ServerException, InterruptedException {
        List<CredentialGetter> credentialList = List.of(this::getTokenCredential, this::getFileCredential, this::getNullCredential, this::getUserPasswordCredential);

        for (var getter : credentialList) {
            var credential = getter.get();
            var session = connect(endpoint, credential);
            if (session != null) {
                return new SessionWithCredential(session, credential);
            }
        }

        throw new AssertionError();
    }

    protected Session connect(String endpoint, Credential credential) throws IOException, ServerException, InterruptedException {
        if (credential == null) {
            return null;
        }
        try {
            return SessionBuilder.connect(endpoint).withCredential(credential).create();
        } catch (CoreServiceException e) {
            if (credential == NullCredential.INSTANCE) {
                var code = e.getDiagnosticCode();
                if (code == CoreServiceCode.AUTHENTICATION_ERROR) {
                    LOG.trace("NullCredential authentication error");
                    return null;
                }
            }
            throw e;
        }
    }

    // 1. 環境変数TSURUGI_AUTH_TOKEN
    protected Credential getTokenCredential() {
        Optional<String> authToken = CredentialEnvironment.findTsurugiAuthToken();
        boolean hasAuthToken = authToken.isPresent();
        LOG.trace("default credential 1. env.TSURUGI_AUTH_TOKEN={}", hasAuthToken ? "<not null>" : "null");
        if (hasAuthToken) {
            return new RememberMeCredential(authToken.get());
        }
        return null;
    }

    // 2. 既定の認証情報ファイル
    protected Credential getFileCredential() throws IOException {
        Optional<Path> credentialPath = CredentialEnvironment.findUserHomeCredentialPath();
        if (credentialPath.isEmpty()) {
            LOG.trace("default credential 2. user.home=null");
        } else {
            var path = credentialPath.get();
            boolean exists = Files.exists(path);
            LOG.trace("default credential 2. path={}, exists={}", path, exists);
            if (exists) {
                return FileCredential.load(path);
            }
        }
        return null;
    }

    // 3. 認証なし
    protected Credential getNullCredential() {
        LOG.trace("default credential 3. no auth");
        return NullCredential.INSTANCE;
    }

    // 4. ユーザー入力
    protected Credential getUserPasswordCredential() {
        String user = readUser();
        LOG.trace("default credential 4. user=[{}]", user);
        String password = readPassword();
        return new UsernamePasswordCredential(user, password);
    }

    /**
     * get user from console.
     *
     * @return user
     */
    public @Nonnull String readUser() {
        // do override
        throw new UnsupportedOperationException("not supported readUser");
    }

    /**
     * get password from console.
     *
     * @return password
     */
    public @Nonnull String readPassword() {
        // do override
        throw new UnsupportedOperationException("not supported readPassword");
    }
}
