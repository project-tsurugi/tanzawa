package com.tsurugidb.console.core.config;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.console.core.credential.CredentialDefaultSupplier;
import com.tsurugidb.console.core.executor.report.HistoryEntry;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.CommitStatus;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;

/**
 * SQL scripts configuration.
 */
public class ScriptConfig {

    private String endpoint;

    private CredentialDefaultSupplier defaultCredentialSupplier;
    private Supplier<Credential> credentialSupplier;
    private Credential credential;

    private SqlRequest.TransactionOption transactionOption;
    private Map<String, String> propertyMap;

    private ScriptCommitMode commitMode;
    private CommitStatus commitStatus;

    private final ScriptClientVariableMap clientVariableMap = new ScriptClientVariableMap();

    private IntFunction<Iterator<HistoryEntry>> historySupplier;

    /**
     * set endpoint.
     *
     * @param endpoint endpoint
     */
    public void setEndpoint(@Nullable String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * get endpoint.
     *
     * @return endpoint
     */
    public @Nullable String getEndpoint() {
        return this.endpoint;
    }

    /**
     * set default credential supplier.
     *
     * @param supplier default credential supplier
     */
    public void setDefaultCredentialSupplier(@Nonnull CredentialDefaultSupplier supplier) {
        Objects.requireNonNull(supplier);
        this.defaultCredentialSupplier = supplier;
    }

    /**
     * get default credential supplier.
     *
     * @return default credential supplier
     */
    public @Nonnull CredentialDefaultSupplier getDefaultCredentialSupplier() {
        return this.defaultCredentialSupplier;
    }

    /**
     * set credential supplier.
     *
     * @param supplier credential supplier
     */
    public void setCredentialSupplier(@Nonnull Supplier<Credential> supplier) {
        Objects.requireNonNull(supplier);
        this.credentialSupplier = supplier;
    }

    /**
     * get credential supplier.
     *
     * @return credential supplier
     */
    public @Nonnull Supplier<Credential> getCredentialSupplier() {
        return this.credentialSupplier;
    }

    /**
     * set credential.
     *
     * @param credential credential
     */
    public void setCredential(@Nullable Credential credential) {
        this.credential = credential;
    }

    /**
     * get credential.
     *
     * @return credential
     */
    public @Nullable Credential getCredential() {
        return this.credential;
    }

    /**
     * set transaction option.
     *
     * @param option transaction option
     */
    public void setTransactionOption(@Nullable SqlRequest.TransactionOption option) {
        this.transactionOption = option;
    }

    /**
     * get transaction option.
     *
     * @return transaction option. if null means --transaction=manual
     */
    public @Nullable SqlRequest.TransactionOption getTransactionOption() {
        return this.transactionOption;
    }

    /**
     * set property.
     *
     * @param property property
     */
    public void setProperty(@Nonnull Map<String, String> property) {
        Objects.requireNonNull(property);
        this.propertyMap = property;
    }

    /**
     * get property.
     *
     * @return property
     */
    public @Nonnull Map<String, String> getProperty() {
        return this.propertyMap;
    }

    /**
     * set commit mode.
     *
     * @param commitMode commit mode
     */
    public void setCommitMode(@Nonnull ScriptCommitMode commitMode) {
        Objects.requireNonNull(commitMode);
        this.commitMode = commitMode;
    }

    /**
     * get commit mode.
     *
     * @return commit mode
     */
    public @Nonnull ScriptCommitMode getCommitMode() {
        return this.commitMode;
    }

    /**
     * set commit status.
     *
     * @param status commit status
     */
    public void setCommitStatus(@Nullable CommitStatus status) {
        this.commitStatus = status;
    }

    /**
     * get commit status.
     *
     * @return commit status
     */
    public @Nullable CommitStatus getCommitStatus() {
        return this.commitStatus;
    }

    /**
     * get client variable.
     *
     * @return client variable
     */
    public @Nonnull ScriptClientVariableMap getClientVariableMap() {
        return this.clientVariableMap;
    }

    /**
     * set history supplier.
     *
     * @param supplier history supplier
     */
    public void setHistorySupplier(IntFunction<Iterator<HistoryEntry>> supplier) {
        this.historySupplier = supplier;
    }

    /**
     * get history supplier.
     *
     * @param size history size
     * @return command history
     */
    public Iterator<HistoryEntry> getHistory(int size) {
        if (this.historySupplier == null) {
            return Collections.emptyIterator();
        }
        return historySupplier.apply(size);
    }
}
