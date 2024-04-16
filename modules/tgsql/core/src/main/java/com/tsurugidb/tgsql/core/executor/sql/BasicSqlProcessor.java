package com.tsurugidb.tgsql.core.executor.sql;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tgsql.core.TgsqlConstants;
import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.model.Region;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.SqlServiceException;
import com.tsurugidb.tsubakuro.sql.StatementMetadata;
import com.tsurugidb.tsubakuro.sql.TableMetadata;
import com.tsurugidb.tsubakuro.sql.exception.TargetNotFoundException;

/**
 * A basic implementation of {@link SqlProcessor}.
 */
public class BasicSqlProcessor implements SqlProcessor {
    static final Logger LOG = LoggerFactory.getLogger(BasicSqlProcessor.class);

    private String sessionEndpoint;
    private Session session;
    private SqlClient sqlClient;
    private TransactionWrapper transaction;

    /**
     * Creates a new instance.
     */
    public BasicSqlProcessor() {
    }

    /**
     * Creates a new instance.
     *
     * @param client the SQL client: It will be closed after this object was closed
     */
    BasicSqlProcessor(@Nonnull SqlClient client) {
        Objects.requireNonNull(client);
        this.session = null;
        this.sqlClient = client;
    }

    @Override
    public void connect(TgsqlConfig config) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(config);

        if (this.sqlClient != null) {
            throw new IllegalStateException("already connected");
        }
        this.sqlClient = SqlClient.attach(getSession(config));
    }

    protected Session getSession(TgsqlConfig config) throws ServerException, IOException, InterruptedException {
        if (this.session == null) {
            String applicationName = TgsqlConstants.APPLICATION_NAME;
            String endpoint = config.getEndpoint();
            if (endpoint == null) {
                throw new IllegalStateException("specify connection-url");
            }
            var label = config.getConnectionLabel();

            Credential credential;
            try {
                var supplier = config.getCredential();
                if (supplier != null) {
                    credential = supplier.get();
                    var builder = SessionBuilder.connect(endpoint).withApplicationName(applicationName).withCredential(credential);
                    label.ifPresent(builder::withLabel);
                    this.session = builder.create();
                } else {
                    var sessionConnector = config.getDefaultCredentialSessionConnector();
                    var connection = sessionConnector.connect(applicationName, label, endpoint);
                    credential = connection.credential();
                    this.session = connection.session();
                }
            } catch (Exception e) {
                LOG.warn("establishing connection: {}", endpoint);
                throw e;
            }
            this.sessionEndpoint = endpoint;
            config.setCredential(() -> credential);
        }
        return this.session;
    }

    protected SqlClient getSqlClient() throws ServerException, IOException, InterruptedException {
        if (this.sqlClient == null) {
            throw new IllegalStateException("connection not exists");
        }
        return this.sqlClient;
    }

    @Override
    public String getEndpoint() {
        return this.sessionEndpoint;
    }

    @Override
    public boolean disconnect() throws ServerException, IOException, InterruptedException {
        return closeSession();
    }

    @Override
    public List<String> getTableNames() throws ServerException, IOException, InterruptedException {
        var client = getSqlClient();
        var tableList = client.listTables().await();
        return tableList.getTableNames();
    }

    @Override
    public TableMetadata getTableMetadata(@Nonnull String tableName) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(tableName);
        try {
            var client = getSqlClient();
            return client.getTableMetadata(tableName).await();
        } catch (TargetNotFoundException e) {
            return null;
        }
    }

    @Override
    public void startTransaction(@Nonnull SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(option);
        desireInactive();
        LOG.debug("start transaction: {}", option);
        var client = getSqlClient();
        var tx = client.createTransaction(option).await();
        this.transaction = new TransactionWrapper(tx, option);
    }

    @Override
    public void commitTransaction(@Nullable SqlRequest.CommitStatus status) throws ServerException, IOException, InterruptedException {
        LOG.debug("start commit: {}", status); //$NON-NLS-1$
        desireActive();
        try (var tx = transaction) {
            transaction = null;
            var t = tx.getTransaction();
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
            try (var tx = transaction) {
                transaction = null;
                var t = tx.getTransaction();
                t.rollback().await();
            }
        } else {
            LOG.warn("rollback request is ignored because transaction is not active");
        }
    }

    @Override
    public @Nullable PreparedStatementResult execute(@Nonnull String statement, @Nullable Region region) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("start prepare: '{}'", statement);
        desireActive();
        var client = getSqlClient();
        try (var prepared = client.prepare(statement).await()) {
            var t = transaction.getTransaction();
            if (prepared.hasResultRecords()) {
                LOG.debug("start query: '{}'", statement);
                var result = t.executeQuery(prepared).await();
                return new PreparedStatementResult(result);
            }
            LOG.debug("start execute: '{}'", statement);
            var result = t.executeStatement(prepared).await();
            return new PreparedStatementResult(result);
        }
    }

    @Override
    public StatementMetadata explain(@Nonnull String statement, @Nonnull Region region) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        Objects.requireNonNull(region);
        // FIXME: SqlClient.explain(String) is not work
        // https://github.com/project-tsurugi/tsubakuro/issues/169
        LOG.debug("start explain: '{}'", statement);
        var client = getSqlClient();
        try (var prepared = client.prepare(statement).await()) {
            return client.explain(prepared, List.of()).await();
        }
    }

    @Override
    public boolean isSessionActive() {
        if (this.session == null) {
            return false;
        }
        return session.isAlive();
    }

    @Override
    public boolean isTransactionActive() {
        return transaction != null;
    }

    @Override
    public String getTransactionId() {
        if (!isTransactionActive()) {
            return null;
        }
        var t = transaction.getTransaction();
        return t.getTransactionId();
    }

    @Override
    public SqlServiceException getTransactionException() throws ServerException, IOException, InterruptedException {
        if (!isTransactionActive()) {
            return null;
        }
        var t = transaction.getTransaction();
        return t.getSqlServiceException().await();
    }

    @Override
    public @Nullable TransactionWrapper getTransaction() {
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
        closeSession();
    }

    private boolean closeSession() throws ServerException, IOException, InterruptedException {
        try (var s = session; var c = sqlClient; var t = transaction) {
            return this.session != null;
        } finally {
            this.sessionEndpoint = null;
            this.session = null;
            this.sqlClient = null;
            this.transaction = null;
        }
    }
}
