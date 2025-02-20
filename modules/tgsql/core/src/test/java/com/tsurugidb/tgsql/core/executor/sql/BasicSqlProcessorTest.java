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
package com.tsurugidb.tgsql.core.executor.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.Placeholder;
import com.tsurugidb.sql.proto.SqlResponse;
import com.tsurugidb.tgsql.core.model.Region;
import com.tsurugidb.tsubakuro.sql.ExecuteResult;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.StatementMetadata;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.impl.BasicStatementMetadata;
import com.tsurugidb.tsubakuro.sql.impl.ResultSetMetadataAdapter;
import com.tsurugidb.tsubakuro.sql.impl.testing.Relation;
import com.tsurugidb.tsubakuro.util.FutureResponse;

class BasicSqlProcessorTest {

    @Test
    void startTransaction() throws Exception {
        Transaction tx = new Transaction() {
            // nothing special
        };
        SqlClient client = new SqlClient() {
            @Override
            public FutureResponse<Transaction> createTransaction(SqlRequest.TransactionOption option) throws IOException {
                return FutureResponse.returns(tx);
            }
        };
        try (var sql = new BasicSqlProcessor(client)) {
            assertFalse(sql.isTransactionActive());
            sql.startTransaction(SqlRequest.TransactionOption.getDefaultInstance());
            assertTrue(sql.isTransactionActive());
            assertSame(tx, sql.getTransaction().getTransaction());
        }
    }

    @Test
    void startTransaction_active_tx() throws Exception {
        Transaction tx = new Transaction() {
            // nothing special
        };
        SqlClient client = new SqlClient() {
            @Override
            public FutureResponse<Transaction> createTransaction(SqlRequest.TransactionOption option) throws IOException {
                return FutureResponse.returns(tx);
            }
        };
        try (var sql = new BasicSqlProcessor(client)) {
            sql.startTransaction(SqlRequest.TransactionOption.getDefaultInstance());
            assertThrows(IllegalStateException.class, () -> sql.startTransaction(SqlRequest.TransactionOption.getDefaultInstance()));
        }
    }

    @Test
    void commitTransaction() throws Exception {
        var reached = new AtomicBoolean();
        Transaction tx = new Transaction() {
            @Override
            public FutureResponse<Void> commit(SqlRequest.CommitStatus status) throws IOException {
                if (!reached.compareAndSet(false, true)) {
                    throw new AssertionError();
                }
                assertEquals(SqlRequest.CommitStatus.STORED, status);
                return FutureResponse.returns(null);
            }
        };
        SqlClient client = new SqlClient() {
            @Override
            public FutureResponse<Transaction> createTransaction(SqlRequest.TransactionOption option) throws IOException {
                return FutureResponse.returns(tx);
            }
        };
        try (var sql = new BasicSqlProcessor(client)) {
            assertFalse(sql.isTransactionActive());
            sql.startTransaction(SqlRequest.TransactionOption.getDefaultInstance());
            assertTrue(sql.isTransactionActive());
            sql.commitTransaction(SqlRequest.CommitStatus.STORED);
            assertFalse(sql.isTransactionActive());
        }
        assertTrue(reached.get());
    }

    @Test
    void commitTransaction_inactive_tx() throws Exception {
        SqlClient client = new SqlClient() {
            // nothing special
        };
        try (var sql = new BasicSqlProcessor(client)) {
            assertThrows(IllegalStateException.class, () -> sql.commitTransaction(null));
        }
    }

    @Test
    void rollbackTransaction() throws Exception {
        var reached = new AtomicBoolean();
        Transaction tx = new Transaction() {
            @Override
            public FutureResponse<Void> rollback() throws IOException {
                if (!reached.compareAndSet(false, true)) {
                    throw new AssertionError();
                }
                return FutureResponse.returns(null);
            }
        };
        SqlClient client = new SqlClient() {
            @Override
            public FutureResponse<Transaction> createTransaction(SqlRequest.TransactionOption option) throws IOException {
                return FutureResponse.returns(tx);
            }
        };
        try (var sql = new BasicSqlProcessor(client)) {
            assertFalse(sql.isTransactionActive());
            sql.startTransaction(SqlRequest.TransactionOption.getDefaultInstance());
            assertTrue(sql.isTransactionActive());
            sql.rollbackTransaction();
            assertFalse(sql.isTransactionActive());
        }
        assertTrue(reached.get());
    }

    @Test
    void rollbackTransaction_inactive_tx() throws Exception {
        SqlClient client = new SqlClient() {
            // nothing special
        };
        try (var sql = new BasicSqlProcessor(client)) {
            sql.rollbackTransaction();
        }
    }

    @Test
    void execute_wo_result() throws Exception {
        var reached = new AtomicBoolean();
        PreparedStatement ps = createPreparedStatement(false);
        Transaction tx = new Transaction() {
            @Override
            public FutureResponse<ExecuteResult> executeStatement(PreparedStatement statement, Collection<? extends SqlRequest.Parameter> parameters) throws IOException {
                if (!reached.compareAndSet(false, true)) {
                    throw new AssertionError();
                }
                assertSame(ps, statement);
                return FutureResponse.returns(null);
            }
        };
        SqlClient client = new SqlClient() {
            @Override
            public FutureResponse<Transaction> createTransaction(SqlRequest.TransactionOption option) throws IOException {
                return FutureResponse.returns(tx);
            }

            @Override
            public FutureResponse<PreparedStatement> prepare(String source, Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                return FutureResponse.returns(ps);
            }
        };
        try (var sql = new BasicSqlProcessor(client)) {
            sql.startTransaction(SqlRequest.TransactionOption.getDefaultInstance());
            try (var result = sql.execute("", new Region(0, 0, 0, 0))) {
                var rs = result.getResultSet();
                assertNull(rs);
            }
        }
        assertTrue(reached.get());
    }

    @Test
    void execute_w_result() throws Exception {
        var reached = new AtomicBoolean();
        ResultSet r = Relation.of().getResultSet(new ResultSetMetadataAdapter(SqlResponse.ResultSetMetadata.getDefaultInstance()));
        PreparedStatement ps = createPreparedStatement(true);
        Transaction tx = new Transaction() {
            @Override
            public FutureResponse<ResultSet> executeQuery(PreparedStatement statement, Collection<? extends SqlRequest.Parameter> parameters) throws IOException {
                if (!reached.compareAndSet(false, true)) {
                    throw new AssertionError();
                }
                assertSame(ps, statement);
                return FutureResponse.returns(r);
            }
        };
        SqlClient client = new SqlClient() {
            @Override
            public FutureResponse<Transaction> createTransaction(SqlRequest.TransactionOption option) throws IOException {
                return FutureResponse.returns(tx);
            }

            @Override
            public FutureResponse<PreparedStatement> prepare(String source, Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                return FutureResponse.returns(ps);
            }
        };
        try (var sql = new BasicSqlProcessor(client)) {
            sql.startTransaction(SqlRequest.TransactionOption.getDefaultInstance());
            try (var result = sql.execute("", new Region(0, 0, 0, 0))) {
                var rs = result.getResultSet();
                assertSame(r, rs);
            }
        }
        assertTrue(reached.get());
    }

    @Test
    void execute_inactive_tx() throws Exception {
        SqlClient client = new SqlClient() {
            // nothing special
        };
        try (var sql = new BasicSqlProcessor(client)) {
            assertThrows(IllegalStateException.class, () -> sql.execute("", new Region(0, 0, 0, 0)));
        }
    }

    @Test
    void explain() throws Exception {
        var client = new SqlClient() {
            private final PreparedStatement preparedStatement = createPreparedStatement(true);
            private String preparedSource;

            @Override
            public FutureResponse<StatementMetadata> explain(String source) throws IOException {
                return FutureResponse.returns(new BasicStatementMetadata("FID", -1, source, List.of()));
            }

            @Override
            public FutureResponse<PreparedStatement> prepare(String source, Collection<? extends Placeholder> placeholders) throws IOException {
                assertNull(preparedSource);
                preparedSource = source;
                return FutureResponse.returns(preparedStatement);
            }

            @Override
            public FutureResponse<StatementMetadata> explain(PreparedStatement statement, Collection<? extends SqlRequest.Parameter> parameters) throws IOException {
                assertSame(preparedStatement, statement);
                ;
                return explain(preparedSource);
            }
        };
        try (var sql = new BasicSqlProcessor(client)) {
            var metadata = sql.explain("SELECT 1", new Region(0, 0, 0, 0));
            assertEquals("FID", metadata.getFormatId());
            assertEquals(-1, metadata.getFormatVersion());
            assertEquals("SELECT 1", metadata.getContents());
        }
    }

    private static PreparedStatement createPreparedStatement(boolean hasResult) {
        return new PreparedStatement() {

            @Override
            public void setCloseTimeout(long timeout, TimeUnit unit) {
                return;
            }

            @Override
            public boolean hasResultRecords() {
                return hasResult;
            }

            @Override
            public void close() {
                return;
            }
        };
    }
}
