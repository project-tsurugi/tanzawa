package com.tsurugidb.console.core.credential;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.FileCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

/**
 * Tsurugi default credential supplier.
 */
public class CredentialDefaultSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(CredentialDefaultSupplier.class);

    /**
     * get default credential.
     *
     * @return credential
     */
    public Credential getDefaultCredential() {
        // 1. 環境変数TSURUGI_AUTH_TOKEN
        Optional<String> authToken = CredentialEnvironment.findTsurugiAuthToken();
        boolean hasAuthToken = authToken.isPresent();
        LOG.trace("default credential 1. env.TSURUGI_AUTH_TOKEN={}", hasAuthToken ? "<not null>" : "null");
        if (hasAuthToken) {
            return new RememberMeCredential(authToken.get());
        }

        // 2. 既定の認証情報ファイル
        Optional<Path> credentialPath = CredentialEnvironment.findUserHomeCredentialPath();
        if (credentialPath.isEmpty()) {
            LOG.trace("default credential 2. user.home=null");
        } else {
            var path = credentialPath.get();
            boolean exists = Files.exists(path);
            LOG.trace("default credential 2. path={}, exists={}", path, exists);
            if (exists) {
                try {
                    return FileCredential.load(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e.getMessage(), e);
                }
            }
        }

        // 3. ユーザー入力
        String user = readUser();
        LOG.trace("default credential 3. user=[{}]", user);
        if (!user.isEmpty()) {
            String password = readPassword();
            return new UsernamePasswordCredential(user, password);
        }

        // 4. 認証なし
        LOG.trace("default credential 4. no auth");
        return NullCredential.INSTANCE;
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
