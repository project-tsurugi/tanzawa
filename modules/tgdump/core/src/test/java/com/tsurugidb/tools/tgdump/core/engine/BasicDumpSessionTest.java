/*
 * Copyright 2023-2024 Project Tsurugi.
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
package com.tsurugidb.tools.tgdump.core.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.tgdump.core.model.DumpProfile;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;
import com.tsurugidb.tools.tgdump.core.model.TransactionSettings;
import com.tsurugidb.tsubakuro.exception.CoreServiceCode;
import com.tsurugidb.tsubakuro.exception.CoreServiceException;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.SqlServiceCode;
import com.tsurugidb.tsubakuro.sql.TableMetadata;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.exception.OccException;
import com.tsurugidb.tsubakuro.util.FutureResponse;

class BasicDumpSessionTest {

    private final TransactionSettings.Builder tx = TransactionSettings.newBuilder();

    private final DumpProfile.Builder profile = DumpProfile.newBuilder();

    private final MockDumpMonitor monitor = new MockDumpMonitor();

    private static DumpTarget table(String tableName) {
        return new DumpTarget(DumpTarget.TargetType.TABLE, tableName, tableName, destination(tableName));
    }

    private static DumpTarget query(String label, String sql) {
        return new DumpTarget(DumpTarget.TargetType.QUERY, label, sql, destination(label));
    }

    private static Path destination(String tableName) {
        return Path.of(String.valueOf(tableName.hashCode())).toAbsolutePath();
    }

    static Path dumpFile(Path directory, int index) {
        return directory.resolve(String.valueOf(index));
    }

    private BasicDumpSession createSession(MockSqlClient client) {
        return new BasicDumpSession(client, tx.build(), profile.build(), false);
    }

    @Test
    void register() throws Exception {
        var registered = new ArrayList<String>();
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<TableMetadata> getTableMetadata(String tableName) throws IOException {
                    registered.add(tableName);
                    return super.getTableMetadata(tableName);
                }
            };
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            assertEquals(BasicDumpSession.State.PREPARING, session.getState());
            assertEquals(List.of("T1"), registered);
            assertEquals(Map.of("T1", destination("T1")), monitor.getInfo());
        }
    }

    @Test
    void register_mixed() throws Exception {
        var registered = new HashSet<String>();
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<TableMetadata> getTableMetadata(String tableName) throws IOException {
                    registered.add(tableName);
                    return super.getTableMetadata(tableName);
                }
                @Override
                public FutureResponse<PreparedStatement> prepare(String source, Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    registered.add(source);
                    return super.prepare(source, placeholders);
                }
            };
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.register(monitor, query("Q1", "SELECT 1"));
            assertEquals(BasicDumpSession.State.PREPARING, session.getState());
            assertEquals(Set.of("T1", "SELECT 1"), registered);
            assertEquals(
                    Map.of(
                            "T1", destination("T1"),
                            "Q1", destination("Q1")),
                    monitor.getInfo());
        }
    }

    @Test
    void begin() throws Exception {
        tx.withType(TransactionSettings.Type.LTX).withEnableReadAreas(true);
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<Transaction> createTransaction(SqlRequest.TransactionOption option) throws IOException {
                    assertEquals(SqlRequest.TransactionType.LONG, option.getType());
                    assertEquals(1, option.getInclusiveReadAreasCount());
                    return super.createTransaction(option);
                }
            };
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            assertEquals(BasicDumpSession.State.RUNNING, session.getState());
        }
    }

    @Test
    void begin_io_error() throws Exception {
        tx.withType(TransactionSettings.Type.LTX).withEnableReadAreas(true);
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<Transaction> createTransaction(SqlRequest.TransactionOption option) throws IOException {
                    throw new IOException();
                }
            };
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            var e = assertThrows(DumpException.class, () -> session.begin(monitor));
            assertEquals(DumpDiagnosticCode.IO_ERROR, e.getDiagnosticCode());
            assertEquals(BasicDumpSession.State.FAILED, session.getState());
        }
    }

    @Test
    void begin_server_error() throws Exception {
        tx.withType(TransactionSettings.Type.LTX).withEnableReadAreas(true);
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<Transaction> createTransaction(SqlRequest.TransactionOption option) throws IOException {
                    return FutureResponse.raises(new CoreServiceException(CoreServiceCode.SERVICE_UNAVAILABLE));
                }
            };
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            var e = assertThrows(DumpException.class, () -> session.begin(monitor));
            assertEquals(DumpDiagnosticCode.BEGIN_FAILURE, e.getDiagnosticCode());
            assertEquals(BasicDumpSession.State.FAILED, session.getState());
        }
    }

    @Test
    void begin_no_register() throws Exception {
        tx.withType(TransactionSettings.Type.LTX).withEnableReadAreas(true);
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<Transaction> createTransaction(SqlRequest.TransactionOption option) throws IOException {
                    throw new IOException();
                }
            };
            var session = createSession(client);
        ) {
            assertThrows(IllegalStateException.class, () -> session.begin(monitor));
            assertEquals(BasicDumpSession.State.FAILED, session.getState());
        }
    }

    @Test
    void register_after_begin() throws Exception {
        try (
            var client = new MockSqlClient();
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            assertThrows(IllegalStateException.class, () -> session.register(monitor, table("T1")));
        }
    }

    @Test
    void begin_after_begin() throws Exception {
        tx.withType(TransactionSettings.Type.LTX).withEnableReadAreas(true);
        try (
            var client = new MockSqlClient();
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            assertThrows(IllegalStateException.class, () -> session.begin(monitor));
            assertEquals(BasicDumpSession.State.RUNNING, session.getState());
        }
    }

    @Test
    void execute() throws Exception {
        var statements = new ArrayList<String>();
        try (
            var client = new MockSqlClient(new MockTransaction() {
                @Override
                public FutureResponse<ResultSet> executeDump(
                        PreparedStatement statement,
                        Collection<? extends SqlRequest.Parameter> parameters,
                        Path directory,
                        SqlRequest.DumpOption option) throws IOException {
                    statements.add(((MockPreparedStatement) statement).getStatement());
                    return super.executeDump(statement, parameters, directory, option);
                }
            });
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            session.execute(monitor, table("T1"));
            assertEquals(Map.of("T1", List.of(dumpFile(destination("T1"), 1))), monitor.getFiles());
            assertEquals(Map.of("T1", destination("T1")), monitor.getStart());
            assertEquals(Map.of("T1", destination("T1")), monitor.getFinish());
            assertEquals(BasicDumpSession.State.RUNNING, session.getState());

            assertEquals(List.of("SELECT * FROM T1"), statements);
        }
    }

    @Test
    void execute_mixed() throws Exception {
        var statements = new HashSet<String>();
        try (
            var client = new MockSqlClient(new MockTransaction() {
                @Override
                public FutureResponse<ResultSet> executeDump(
                        PreparedStatement statement,
                        Collection<? extends SqlRequest.Parameter> parameters,
                        Path directory,
                        SqlRequest.DumpOption option) throws IOException {
                    statements.add(((MockPreparedStatement) statement).getStatement());
                    return super.executeDump(statement, parameters, directory, option);
                }
            });
            var session = createSession(client);
        ) {
            var t1 = table("T1");
            var t2 = query("Q1", "SELECT 1");

            session.register(monitor, t1);
            session.register(monitor, t2);
            session.begin(monitor);
            session.execute(monitor, t1);
            session.execute(monitor, t2);
            assertEquals(
                    Map.of(
                            "T1", List.of(dumpFile(destination("T1"), 1)),
                            "Q1", List.of(dumpFile(destination("Q1"), 1))),
                    monitor.getFiles());
            assertEquals(
                    Map.of(
                            "T1", destination("T1"),
                            "Q1", destination("Q1")),
                    monitor.getStart());
            assertEquals(
                    Map.of(
                            "T1", destination("T1"),
                            "Q1", destination("Q1")),
                    monitor.getFinish());
            assertEquals(BasicDumpSession.State.RUNNING, session.getState());

            assertEquals(Set.of("SELECT * FROM T1", "SELECT 1"), statements);
        }
    }

    @Test
    void execute_before_begin() throws Exception {
        try (
            var client = new MockSqlClient();
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            assertThrows(IllegalStateException.class, () -> session.execute(monitor, table("T1")));
        }
    }

    @Test
    void execute_without_register() throws Exception {
        try (
            var client = new MockSqlClient();
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            assertThrows(IllegalArgumentException.class, () -> session.execute(monitor, table("XXX")));
        }
    }

    @Test
    void commit() throws Exception {
        var committedRef = new AtomicBoolean();
        try (
            var client = new MockSqlClient(new MockTransaction() {
                @Override
                public FutureResponse<Void> commit(SqlRequest.CommitStatus status) throws IOException {
                    assertTrue(committedRef.compareAndSet(false, true));
                    return super.commit(status);
                }
            });
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            session.execute(monitor, table("T1"));
            session.commit(monitor);
            assertEquals(BasicDumpSession.State.COMMITTED, session.getState());
            assertTrue(committedRef.get());
        }
    }

    @Test
    void commit_server_error() throws Exception {
        try (
            var client = new MockSqlClient(new MockTransaction() {
                @Override
                public FutureResponse<Void> commit(SqlRequest.CommitStatus status) throws IOException {
                    return FutureResponse.raises(new OccException(SqlServiceCode.OCC_EXCEPTION));
                }
            });
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            session.execute(monitor, table("T1"));
            var e = assertThrows(DiagnosticException.class, () -> session.commit(monitor));
            assertEquals(DumpDiagnosticCode.COMMIT_FAILURE, e.getDiagnosticCode());
            assertEquals(BasicDumpSession.State.FAILED, session.getState());
        }
    }

    @Test
    void commit_io_error() throws Exception {
        try (
            var client = new MockSqlClient(new MockTransaction() {
                @Override
                public FutureResponse<Void> commit(SqlRequest.CommitStatus status) throws IOException {
                    throw new IOException();
                }
            });
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            session.execute(monitor, table("T1"));
            var e = assertThrows(DiagnosticException.class, () -> session.commit(monitor));
            assertEquals(DumpDiagnosticCode.IO_ERROR, e.getDiagnosticCode());
            assertEquals(BasicDumpSession.State.FAILED, session.getState());
        }
    }

    @Test
    void commit_before_begin() throws Exception {
        try (
            var client = new MockSqlClient();
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            assertThrows(IllegalStateException.class, () -> session.commit(monitor));
        }
    }

    @Test
    void close() throws Exception {
        try (
            var client = new MockSqlClient();
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            session.execute(monitor, table("T1"));
            session.commit(monitor);
            session.close();
            assertEquals(BasicDumpSession.State.CLOSED, session.getState());

            // idempotent
            session.close();
        }
    }

    @Test
    void close_before_commit() throws Exception {
        var closedRef = new AtomicBoolean();
        try (
            var client = new MockSqlClient(new MockTransaction() {
                @Override
                public void close() {
                    closedRef.set(true);
                }
            });
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            session.execute(monitor, table("T1"));
            session.close();
            assertTrue(closedRef.get());
        }
    }

    @Test
    void close_server_error() throws Exception {
        try (
            var client = new MockSqlClient(new MockTransaction() {
                @Override
                public void close() throws ServerException {
                    throw new CoreServiceException(CoreServiceCode.INVALID_REQUEST);
                }
            });
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            session.execute(monitor, table("T1"));
            session.commit(monitor);
            var e = assertThrows(DiagnosticException.class, () -> session.close());
            assertEquals(DumpDiagnosticCode.SERVER_ERROR, e.getDiagnosticCode());
        }
    }

    @Test
    void close_io_error() throws Exception {
        try (
            var client = new MockSqlClient(new MockTransaction() {
                @Override
                public void close() throws IOException {
                    throw new IOException();
                }
            });
            var session = createSession(client);
        ) {
            session.register(monitor, table("T1"));
            session.begin(monitor);
            session.execute(monitor, table("T1"));
            session.commit(monitor);
            var e = assertThrows(DiagnosticException.class, () -> session.close());
            assertEquals(DumpDiagnosticCode.IO_ERROR, e.getDiagnosticCode());
        }
    }
}
