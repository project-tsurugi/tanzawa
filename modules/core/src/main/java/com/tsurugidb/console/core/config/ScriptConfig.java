package com.tsurugidb.console.core.config;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;

/**
 * SQL scripts configuration.
 */
public class ScriptConfig {

    private URI endpoint;

    private Credential credential;

    private SqlRequest.TransactionOption transactionOption;

    private Map<String, String> propertyMap;

    private ScriptCommitMode commitMode;

    public void setEndpoint(@Nonnull URI endpoint) {
        Objects.requireNonNull(endpoint);
        this.endpoint = endpoint;
    }

    @Nonnull
    public URI getEndpoint() {
        return this.endpoint;
    }

    public void setCredential(@Nonnull Credential credential) {
        Objects.requireNonNull(credential);
        this.credential = credential;
    }

    @Nonnull
    public Credential getCredential() {
        return this.credential;
    }

    public void setTransactionOption(@Nullable SqlRequest.TransactionOption option) {
        this.transactionOption = option;
    }

    /**
     * @return transaction option. if null means --transaction=manual
     */
    @Nullable
    public SqlRequest.TransactionOption getTransactionOption() {
        return this.transactionOption;
    }

    public void setProperty(@Nonnull Map<String, String> property) {
        Objects.requireNonNull(property);
        this.propertyMap = property;
    }

    @Nonnull
    public Map<String, String> getProperty() {
        return this.propertyMap;
    }

    public void setCommitMode(@Nonnull ScriptCommitMode commitMode) {
        Objects.requireNonNull(commitMode);
        this.commitMode = commitMode;
    }

    @Nonnull
    public ScriptCommitMode getCommitMode() {
        return this.commitMode;
    }
}
