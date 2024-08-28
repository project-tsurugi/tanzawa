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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.tsurugidb.sql.proto.SqlCommon;
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
import com.tsurugidb.tsubakuro.sql.ResultSetMetadata;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.SqlServiceCode;
import com.tsurugidb.tsubakuro.sql.TableMetadata;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.Types;
import com.tsurugidb.tsubakuro.sql.exception.EvaluationException;
import com.tsurugidb.tsubakuro.sql.exception.OccException;
import com.tsurugidb.tsubakuro.sql.exception.SyntaxException;
import com.tsurugidb.tsubakuro.sql.exception.TargetNotFoundException;
import com.tsurugidb.tsubakuro.sql.impl.EmptyRelationCursor;
import com.tsurugidb.tsubakuro.util.FutureResponse;

class BasicDumpSessionTest {

    static class MockSqlClient implements SqlClient {

        private final Transaction tx;

        MockSqlClient() {
            this(new MockTransaction());
        }

        MockSqlClient(Transaction tx) {
            this.tx = tx;
        }

        @Override
        public FutureResponse<TableMetadata> getTableMetadata(String tableName) throws IOException {
            return FutureResponse.returns(new MockTableMetadata("T1"));
        }

        @Override
        public FutureResponse<PreparedStatement> prepare(
                String source,
                Collection<? extends SqlRequest.Placeholder> placeholders)
                throws IOException {
            return FutureResponse.returns(new MockPreparedStatement(source));
        }

        @Override
        public FutureResponse<Transaction> createTransaction(SqlRequest.TransactionOption option) throws IOException {
            return FutureResponse.returns(tx);
        }
    }

    static class MockTransaction implements Transaction {

        @Override
        public FutureResponse<ResultSet> executeDump(
                PreparedStatement statement,
                Collection<? extends SqlRequest.Parameter> parameters,
                Path directory,
                SqlRequest.DumpOption option) throws IOException {
            return FutureResponse.returns(new MockResultSet(List.of(dumpFile(directory, 1).toString())));
        }

        @Override
        public FutureResponse<Void> commit(SqlRequest.CommitStatus status) throws IOException {
            return FutureResponse.returns(null);
        }

        @Override
        public String getTransactionId() {
            return "TXID-TESTING";
        }
    }

    static class MockPreparedStatement implements PreparedStatement {

        private final String statement;

        MockPreparedStatement(String statement) {
            this.statement = statement;
        }

        public String getStatement() {
            return statement;
        }

        @Override
        public boolean hasResultRecords() {
            return true;
        }

        @Override
        public void close() {
            return;
        }
    }

    static class MockResultSet extends EmptyRelationCursor implements ResultSet {

        private final List<String> results;

        private int rowPosition;

        private int columnPosition;

        MockResultSet(List<String> results) {
            this.results = results;
            this.rowPosition = -1;
            this.columnPosition = -1;
        }

        @Override
        public ResultSetMetadata getMetadata() throws IOException, ServerException, InterruptedException {
            // just assumes single character column.
            return new ResultSetMetadata() {
                @Override
                public List<? extends SqlCommon.Column> getColumns() {
                    return List.of(Types.column(String.class));
                }
            };
        }

        @Override
        public boolean nextRow() {
            if (rowPosition + 1 < results.size()) {
                rowPosition++;
                columnPosition = -1;
                return true;
            }
            return false;
        }

        @Override
        public boolean nextColumn() {
            if (columnPosition < 0) {
                columnPosition++;
                return true;
            }
            return false;
        }

        @Override
        public String fetchCharacterValue() {
            if (rowPosition < 0 || rowPosition >= results.size() || columnPosition != 0) {
                throw new IllegalStateException();
            }
            return results.get(rowPosition);
        }
    }

    private final TransactionSettings.Builder tx = TransactionSettings.newBuilder();

    private final DumpProfile.Builder profile = DumpProfile.newBuilder();

    private final MockDumpMonitor monitor = new MockDumpMonitor();

    private static DumpTarget target(String tableName) {
        return new DumpTarget(tableName, destination(tableName));
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
            session.register(monitor, target("T1"));
            assertEquals(BasicDumpSession.State.PREPARING, session.getState());
            assertEquals(List.of("T1"), registered);
            assertEquals(Map.of("T1", destination("T1")), monitor.getInfo());
        }
    }

    @Test
    void register_multiple() throws Exception {
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
            session.register(monitor, target("T1"));
            session.register(monitor, target("T2"));
            session.register(monitor, target("T3"));
            assertEquals(BasicDumpSession.State.PREPARING, session.getState());
            assertEquals(List.of("T1", "T2", "T3"), registered);
            assertEquals(
                    Map.of("T1", destination("T1"), "T2", destination("T2"), "T3", destination("T3")),
                    monitor.getInfo());
        }
    }

    @Test
    void register_duplicate() throws Exception {
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
            session.register(monitor, target("T1"));
            session.register(monitor, target("T1"));
            assertEquals(BasicDumpSession.State.PREPARING, session.getState());
            assertEquals(List.of("T1"), registered);
        }
    }

    @Test
    void register_not_found() throws Exception {
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<TableMetadata> getTableMetadata(String tableName) throws IOException {
                    return FutureResponse.raises(new TargetNotFoundException(SqlServiceCode.TARGET_NOT_FOUND_EXCEPTION));
                }
            };
            var session = createSession(client);
        ) {
            var e = assertThrows(DiagnosticException.class, () -> session.register(monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.TABLE_NOT_FOUND, e.getDiagnosticCode());
        }
    }

    @Test
    void register_io_error() throws Exception {
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<TableMetadata> getTableMetadata(String tableName) throws IOException {
                    throw new IOException();
                }
            };
            var session = createSession(client);
        ) {
            var e = assertThrows(DiagnosticException.class, () -> session.register(monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.IO_ERROR, e.getDiagnosticCode());
        }
    }

    @Test
    void register_server_error() throws Exception {
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<TableMetadata> getTableMetadata(String tableName) throws IOException {
                    return FutureResponse.raises(new CoreServiceException(CoreServiceCode.PERMISSION_ERROR));
                }
            };
            var session = createSession(client);
        ) {
            var e = assertThrows(DiagnosticException.class, () -> session.register(monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.SERVER_ERROR, e.getDiagnosticCode());
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
            session.register(monitor, target("T1"));
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
            session.register(monitor, target("T1"));
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
            session.register(monitor, target("T1"));
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
            session.register(monitor, target("T1"));
            session.begin(monitor);
            assertThrows(IllegalStateException.class, () -> session.register(monitor, target("T1")));
        }
    }

    @Test
    void begin_after_begin() throws Exception {
        tx.withType(TransactionSettings.Type.LTX).withEnableReadAreas(true);
        try (
            var client = new MockSqlClient();
            var session = createSession(client);
        ) {
            session.register(monitor, target("T1"));
            session.begin(monitor);
            assertThrows(IllegalStateException.class, () -> session.begin(monitor));
            assertEquals(BasicDumpSession.State.RUNNING, session.getState());
        }
    }

    @Test
    void execute() throws Exception {
        var statements = new ArrayList<String>();
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    statements.add(source);
                    return super.prepare(source, placeholders);
                }
            };
            var session = createSession(client);
        ) {
            session.register(monitor, target("T1"));
            session.begin(monitor);
            session.execute(monitor, target("T1"));
            assertEquals(Map.of("T1", List.of(dumpFile(destination("T1"), 1))), monitor.getFiles());
            assertEquals(Map.of("T1", destination("T1")), monitor.getStart());
            assertEquals(Map.of("T1", destination("T1")), monitor.getFinish());
            assertEquals(BasicDumpSession.State.RUNNING, session.getState());

            assertEquals(List.of("SELECT * FROM T1"), statements);
        }
    }

    @Test
    void execute_multiple_tables() throws Exception {
        try (
            var client = new MockSqlClient();
            var session = createSession(client);
        ) {
            session.register(monitor, target("T1"));
            session.register(monitor, target("T2"));
            session.register(monitor, target("T3"));
            session.begin(monitor);
            session.execute(monitor, target("T3"));
            session.execute(monitor, target("T2"));
            session.execute(monitor, target("T1"));
            assertEquals(
                    Map.of(
                            "T1", List.of(dumpFile(destination("T1"), 1)),
                            "T2", List.of(dumpFile(destination("T2"), 1)),
                            "T3", List.of(dumpFile(destination("T3"), 1))),
                    monitor.getFiles());
            assertEquals(
                    Map.of(
                            "T1", destination("T1"),
                            "T2", destination("T2"),
                            "T3", destination("T3")),
                    monitor.getFinish());
            assertEquals(
                    Map.of(
                            "T1", destination("T1"),
                            "T2", destination("T2"),
                            "T3", destination("T3")),
                    monitor.getStart());
            assertEquals(BasicDumpSession.State.RUNNING, session.getState());
        }
    }

    @Test
    void execute_multiple_files() throws Exception {
        try (
            var client = new MockSqlClient(new MockTransaction() {
                @Override
                public FutureResponse<ResultSet> executeDump(
                        PreparedStatement statement,
                        Collection<? extends SqlRequest.Parameter> parameters,
                        Path directory,
                        SqlRequest.DumpOption option) throws IOException {
                    return FutureResponse.returns(new MockResultSet(List.of(
                            dumpFile(directory, 1).toString(),
                            dumpFile(directory, 2).toString(),
                            dumpFile(directory, 3).toString())));
                }
            });
            var session = createSession(client);
        ) {
            session.register(monitor, target("T1"));
            session.begin(monitor);
            session.execute(monitor, target("T1"));
            assertEquals(
                    Map.of(
                            "T1",
                            List.of(
                                    dumpFile(destination("T1"), 1),
                                    dumpFile(destination("T1"), 2),
                                    dumpFile(destination("T1"), 3))),
                    monitor.getFiles());
            assertEquals(Map.of("T1", destination("T1")), monitor.getStart());
            assertEquals(Map.of("T1", destination("T1")), monitor.getFinish());
            assertEquals(BasicDumpSession.State.RUNNING, session.getState());
        }
    }

    @Test
    void execute_table_name_non_regular() throws Exception {
        var statements = new ArrayList<String>();
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    statements.add(source);
                    return super.prepare(source, placeholders);
                }
            };
            var session = createSession(client);
        ) {
            session.register(monitor, target("T 1"));
            session.begin(monitor);
            session.execute(monitor, target("T 1"));
            assertEquals(List.of("SELECT * FROM \"T 1\""), statements);
        }
    }

    @Test
    void execute_table_name_delimiter() throws Exception {
        var statements = new ArrayList<String>();
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    statements.add(source);
                    return super.prepare(source, placeholders);
                }
            };
            var session = createSession(client);
        ) {
            session.register(monitor, target("T\"1"));
            session.begin(monitor);
            session.execute(monitor, target("T\"1"));
            assertEquals(List.of("SELECT * FROM \"T\"\"1\""), statements);
        }
    }

    @Test
    void execute_prepare_failure() throws Exception {
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    return FutureResponse.raises(new SyntaxException(SqlServiceCode.SYNTAX_EXCEPTION));
                }
            };
            var session = createSession(client);
        ) {
            session.register(monitor, target("T1"));
            session.begin(monitor);
            var e = assertThrows(DiagnosticException.class, () -> session.execute(monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.PREPARE_FAILURE, e.getDiagnosticCode());
        }
    }

    @Test
    void execute_prepare_failure_io_error() throws Exception {
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    throw new IOException();
                }
            };
            var session = createSession(client);
        ) {
            session.register(monitor, target("T1"));
            session.begin(monitor);
            var e = assertThrows(DiagnosticException.class, () -> session.execute(monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.IO_ERROR, e.getDiagnosticCode());
        }
    }

    @Test
    void execute_operation_failure() throws Exception {
        try (
            var client = new MockSqlClient(new MockTransaction() {
                @Override
                public FutureResponse<ResultSet> executeDump(
                        PreparedStatement statement,
                        Collection<? extends SqlRequest.Parameter> parameters,
                        Path directory,
                        SqlRequest.DumpOption option) throws IOException {
                    return FutureResponse.raises(new EvaluationException(SqlServiceCode.EVALUATION_EXCEPTION));
                }
            });
            var session = createSession(client);
        ) {
            session.register(monitor, target("T1"));
            session.begin(monitor);
            var e = assertThrows(DiagnosticException.class, () -> session.execute(monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.OPERATION_FAILURE, e.getDiagnosticCode());
        }
    }

    @Test
    void execute_operation_failure_io_error() throws Exception {
        try (
            var client = new MockSqlClient(new MockTransaction() {
                @Override
                public FutureResponse<ResultSet> executeDump(
                        PreparedStatement statement,
                        Collection<? extends SqlRequest.Parameter> parameters,
                        Path directory,
                        SqlRequest.DumpOption option) throws IOException {
                    throw new IOException();
                }
            });
            var session = createSession(client);
        ) {
            session.register(monitor, target("T1"));
            session.begin(monitor);
            var e = assertThrows(DiagnosticException.class, () -> session.execute(monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.IO_ERROR, e.getDiagnosticCode());
        }
    }

    @Test
    void execute_before_begin() throws Exception {
        try (
            var client = new MockSqlClient();
            var session = createSession(client);
        ) {
            session.register(monitor, target("T1"));
            assertThrows(IllegalStateException.class, () -> session.execute(monitor, target("T1")));
        }
    }

    @Test
    void execute_without_register() throws Exception {
        try (
            var client = new MockSqlClient();
            var session = createSession(client);
        ) {
            session.register(monitor, target("T1"));
            session.begin(monitor);
            assertThrows(IllegalArgumentException.class, () -> session.execute(monitor, target("XXX")));
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
            session.register(monitor, target("T1"));
            session.begin(monitor);
            session.execute(monitor, target("T1"));
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
            session.register(monitor, target("T1"));
            session.begin(monitor);
            session.execute(monitor, target("T1"));
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
            session.register(monitor, target("T1"));
            session.begin(monitor);
            session.execute(monitor, target("T1"));
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
            session.register(monitor, target("T1"));
            assertThrows(IllegalStateException.class, () -> session.commit(monitor));
        }
    }

    @Test
    void close() throws Exception {
        try (
            var client = new MockSqlClient();
            var session = createSession(client);
        ) {
            session.register(monitor, target("T1"));
            session.begin(monitor);
            session.execute(monitor, target("T1"));
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
            session.register(monitor, target("T1"));
            session.begin(monitor);
            session.execute(monitor, target("T1"));
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
            session.register(monitor, target("T1"));
            session.begin(monitor);
            session.execute(monitor, target("T1"));
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
            session.register(monitor, target("T1"));
            session.begin(monitor);
            session.execute(monitor, target("T1"));
            session.commit(monitor);
            var e = assertThrows(DiagnosticException.class, () -> session.close());
            assertEquals(DumpDiagnosticCode.IO_ERROR, e.getDiagnosticCode());
        }
    }
}
