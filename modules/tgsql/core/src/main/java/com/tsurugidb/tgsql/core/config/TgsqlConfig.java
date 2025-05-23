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
package com.tsurugidb.tgsql.core.config;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.CommitStatus;
import com.tsurugidb.tgsql.core.credential.DefaultCredentialSessionConnector;
import com.tsurugidb.tgsql.core.executor.report.HistoryEntry;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;

/**
 * tgsql configuration.
 */
public class TgsqlConfig {

    private String endpoint;
    private Optional<String> connectionLabel = Optional.empty();

    private Supplier<Credential> credential;
    private DefaultCredentialSessionConnector defaultCredentialSessionConnector;

    private SqlRequest.TransactionOption transactionOption;
    private Map<String, String> propertyMap = Map.of();

    private TgsqlCommitMode commitMode;
    private CommitStatus commitStatus;

    private final TgsqlClientVariableMap clientVariableMap = new TgsqlClientVariableMap();

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
     * set connection label.
     *
     * @param label connection label
     */
    public void setConnectionLabel(@Nullable String label) {
        this.connectionLabel = Optional.ofNullable(label);
    }

    /**
     * get connection label.
     *
     * @return connection label
     */
    public Optional<String> getConnectionLabel() {
        return this.connectionLabel;
    }

    /**
     * set credential.
     *
     * @param credential credential
     */
    public void setCredential(@Nullable Supplier<Credential> credential) {
        this.credential = credential;
    }

    /**
     * get credential.
     *
     * @return credential
     */
    public @Nullable Supplier<Credential> getCredential() {
        return this.credential;
    }

    /**
     * set default credential session connector.
     *
     * @param sessionConnector default credential session connector
     */
    public void setDefaultCredentialSessionConnector(@Nonnull DefaultCredentialSessionConnector sessionConnector) {
        Objects.requireNonNull(sessionConnector);
        this.defaultCredentialSessionConnector = sessionConnector;
    }

    /**
     * get default credential session connector.
     *
     * @return default credential session connector
     */
    public @Nonnull DefaultCredentialSessionConnector getDefaultCredentialSessionConnector() {
        return this.defaultCredentialSessionConnector;
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
    public void setCommitMode(@Nonnull TgsqlCommitMode commitMode) {
        Objects.requireNonNull(commitMode);
        this.commitMode = commitMode;
    }

    /**
     * get commit mode.
     *
     * @return commit mode
     */
    public @Nonnull TgsqlCommitMode getCommitMode() {
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
    public @Nonnull TgsqlClientVariableMap getClientVariableMap() {
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
