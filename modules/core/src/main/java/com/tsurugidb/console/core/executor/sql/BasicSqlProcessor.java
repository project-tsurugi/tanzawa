package com.tsurugidb.console.core.executor.sql;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.model.Region;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.SqlServiceCode;
import com.tsurugidb.tsubakuro.sql.StatementMetadata;
import com.tsurugidb.tsubakuro.sql.TableMetadata;
import com.tsurugidb.tsubakuro.sql.Transaction;

/**
 * A basic implementation of {@link SqlProcessor}.
 */
public class BasicSqlProcessor implements SqlProcessor {

    static final Logger LOG = LoggerFactory.getLogger(BasicSqlProcessor.class);

    private final SqlClient client;

    private Transaction transaction;

    /**
     * Creates a new instance.
     *
     * @param client the SQL client: It will be closed after this object was closed
     */
    public BasicSqlProcessor(@Nonnull SqlClient client) {
        Objects.requireNonNull(client);
        this.client = client;
    }

    @Override
    public TableMetadata getTableMetadata(@Nonnull String tableName) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(tableName);
        try {
            return client.getTableMetadata(tableName).await();
        } catch (ServerException e) {
            var code = e.getDiagnosticCode();
            if (code == SqlServiceCode.ERR_NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public void startTransaction(@Nonnull SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(option);
        desireInactive();
        LOG.debug("start transaction: {}", option);
        transaction = client.createTransaction(option).await();
    }

    @Override
    public void commitTransaction(@Nullable SqlRequest.CommitStatus status) throws ServerException, IOException, InterruptedException {
        LOG.debug("start commit: {}", status); //$NON-NLS-1$
        desireActive();
        try (var t = transaction) {
            transaction = null;
            if (status == null) {
                t.commit().await();
            } else {
                t.commit(status).await();
            }
        }
    }

    @Override
    public void rollbackTransaction() throws ServerException, IOException, InterruptedException {
        LOG.debug("start rollback"); //$NON-NLS-1$
        if (isTransactionActive()) {
            try (var t = transaction) {
                transaction = null;
                t.rollback().await();
            }
        } else {
            LOG.warn("rollback request is ignored because transaction is not active");
        }
    }

    @Override
    public @Nullable ResultSet execute(@Nonnull String statement, @Nullable Region region) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("start prepare: '{}'", statement);
        desireActive();
        try (var prepared = client.prepare(statement).await()) {
            if (prepared.hasResultRecords()) {
                LOG.debug("start query: '{}'", statement);
                return transaction.executeQuery(prepared).await();
            }
            LOG.debug("start execute: '{}'", statement);
            transaction.executeStatement(prepared).await();
            return null;
        }
    }

    @Override
    public StatementMetadata explain(@Nonnull String statement, @Nonnull Region region)
            throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        Objects.requireNonNull(region);
        // FIXME: SqlClient.explain(String) is not work
        // https://github.com/project-tsurugi/tsubakuro/issues/169
        LOG.debug("start explain: '{}'", statement);
        try (var prepared = client.prepare(statement).await()) {
            return client.explain(prepared, List.of()).await();
        }
    }

    @Override
    public boolean isTransactionActive() {
        return transaction != null;
    }

    /**
     * Returns the running transaction.
     *
     * @return the running transaction, or {@code null} if there is no active transactions
     */
    public @Nullable Transaction getTransaction() {
        return transaction;
    }

    private void desireActive() {
        if (!isTransactionActive()) {
            throw new IllegalStateException("transaction is not running");
        }
    }

    private void desireInactive() {
        if (isTransactionActive()) {
            throw new IllegalStateException("transaction is running");
        }
    }

    @Override
    public void close() throws ServerException, IOException, InterruptedException {
        try (var t = transaction; var c = client) {
            return;
        }
    }
}
