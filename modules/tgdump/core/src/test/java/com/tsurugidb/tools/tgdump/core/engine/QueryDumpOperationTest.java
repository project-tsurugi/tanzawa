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

import org.junit.jupiter.api.Test;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.tgdump.core.model.DumpProfile;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;
import com.tsurugidb.tsubakuro.exception.CoreServiceCode;
import com.tsurugidb.tsubakuro.exception.CoreServiceException;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.SqlServiceCode;
import com.tsurugidb.tsubakuro.sql.exception.EvaluationException;
import com.tsurugidb.tsubakuro.sql.exception.SyntaxException;
import com.tsurugidb.tsubakuro.util.FutureResponse;

class QueryDumpOperationTest {

    private final DumpProfile.Builder profile = DumpProfile.newBuilder();

    private final MockDumpMonitor monitor = new MockDumpMonitor();

    private static DumpTarget target(String label, String query) {
        return new DumpTarget(DumpTarget.TargetType.QUERY, label, query, destination(label));
    }

    private static DumpTarget target(String tableName) {
        return target(tableName, query(tableName));
    }

    private static String query(String tableName) {
        return String.format("SELECT * FROM %s", tableName);
    }

    private static Path destination(String tableName) {
        return Path.of(String.valueOf(tableName.hashCode())).toAbsolutePath();
    }

    static Path dumpFile(Path directory, int index) {
        return directory.resolve(String.valueOf(index));
    }

    private QueryDumpOperation createOperation() {
        return new QueryDumpOperation(profile.build(), false);
    }

    @Test
    void register() throws Exception {
        var operation = createOperation();
        var registered = new ArrayList<String>();
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    registered.add(source);
                    return super.prepare(source, placeholders);
                }
            };
        ) {
            assertTrue(operation.isEmpty());
            assertEquals(operation.getTargetTables(), List.of());
            operation.register(client, monitor, target("T1"));
            assertEquals(List.of(query("T1")), registered);
            assertEquals(Map.of("T1", destination("T1")), monitor.getInfo());
            assertFalse(operation.isEmpty());
            assertEquals(operation.getTargetTables(), List.of());
        }
    }

    @Test
    void register_multiple() throws Exception {
        var operation = createOperation();
        var registered = new ArrayList<String>();
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    registered.add(source);
                    return super.prepare(source, placeholders);
                }
            }
        ) {
            assertTrue(operation.isEmpty());
            assertEquals(operation.getTargetTables(), List.of());
            operation.register(client, monitor, target("T1"));
            operation.register(client, monitor, target("T2"));
            operation.register(client, monitor, target("T3"));
            assertEquals(
                    List.of(query("T1"), query("T2"), query("T3")),
                    registered);
            assertEquals(
                    Map.of("T1", destination("T1"), "T2", destination("T2"), "T3", destination("T3")),
                    monitor.getInfo());
            assertFalse(operation.isEmpty());
            assertEquals(operation.getTargetTables(), List.of());
        }
    }

    @Test
    void register_duplicate() throws Exception {
        var operation = createOperation();
        var registered = new ArrayList<String>();
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    registered.add(source);
                    return super.prepare(source, placeholders);
                }
            }
        ) {
            operation.register(client, monitor, target("a", "SELECT 1"));
            operation.register(client, monitor, target("b", "SELECT 1"));
            assertEquals(List.of("SELECT 1"), registered);
        }
    }

    @Test
    void register_compile_error() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    return FutureResponse.raises(new SyntaxException(SqlServiceCode.SYNTAX_EXCEPTION));
                }
            }
        ) {
            var e = assertThrows(DiagnosticException.class, () -> operation.register(client, monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.PREPARE_FAILURE, e.getDiagnosticCode());
        }
    }

    @Test
    void register_io_error() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    throw new IOException();
                }
            }
        ) {
            var e = assertThrows(DiagnosticException.class, () -> operation.register(client, monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.IO_ERROR, e.getDiagnosticCode());
        }
    }

    @Test
    void register_server_error() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient() {
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    return FutureResponse.raises(new CoreServiceException(CoreServiceCode.PERMISSION_ERROR));
                }
            }
        ) {
            var e = assertThrows(DiagnosticException.class, () -> operation.register(client, monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.SERVER_ERROR, e.getDiagnosticCode());
        }
    }

    @Test
    void register_unsupported_target_type() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient();
        ) {
            var target = new DumpTarget(DumpTarget.TargetType.TABLE, "T1", "T1", destination("mismatch"));
            assertThrows(UnsupportedOperationException.class, () -> operation.register(client, monitor, target));
        }
    }

    @Test
    void execute() throws Exception {
        var operation = createOperation();
        var statements = new HashSet<String>();
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
            var tx = new MockTransaction();
        ) {
            operation.register(client, monitor, target("T1"));
            operation.execute(client, tx, monitor, target("T1"));
            assertEquals(Map.of("T1", List.of(dumpFile(destination("T1"), 1))), monitor.getFiles());
            assertEquals(Map.of("T1", destination("T1")), monitor.getStart());
            assertEquals(Map.of("T1", destination("T1")), monitor.getFinish());

            assertEquals(Set.of("SELECT * FROM T1"), statements);
        }
    }

    @Test
    void execute_multiple_tables() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient();
            var tx = new MockTransaction();
        ) {
            operation.register(client, monitor, target("T1"));
            operation.register(client, monitor, target("T2"));
            operation.register(client, monitor, target("T3"));
            operation.execute(client, tx, monitor, target("T3"));
            operation.execute(client, tx, monitor, target("T2"));
            operation.execute(client, tx, monitor, target("T1"));
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
        }
    }

    @Test
    void execute_multiple_files() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient();
            var tx = new MockTransaction() {
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
            };
        ) {
            operation.register(client, monitor, target("T1"));
            operation.execute(client, tx, monitor, target("T1"));
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
        }
    }

    @Test
    void execute_prepare_failure() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient() {
                boolean first = true;
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    if (first) {
                        first = false;
                        return super.prepare(source, placeholders);
                    }
                    return FutureResponse.raises(new SyntaxException(SqlServiceCode.SYNTAX_EXCEPTION));
                }
            };
            var tx = new MockTransaction();
        ) {
            operation.register(client, monitor, target("T1"));
            var e = assertThrows(DiagnosticException.class, () -> operation.execute(client, tx, monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.PREPARE_FAILURE, e.getDiagnosticCode());
        }
    }

    @Test
    void execute_prepare_failure_io_error() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient() {
                boolean first = true;
                @Override
                public FutureResponse<PreparedStatement> prepare(
                        String source,
                        Collection<? extends SqlRequest.Placeholder> placeholders) throws IOException {
                    if (first) {
                        first = false;
                        return super.prepare(source, placeholders);
                    }
                    throw new IOException();
                }
            };
            var tx = new MockTransaction();
        ) {
            operation.register(client, monitor, target("T1"));
            var e = assertThrows(DiagnosticException.class, () -> operation.execute(client, tx, monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.IO_ERROR, e.getDiagnosticCode());
        }
    }

    @Test
    void execute_operation_failure() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient();
            var tx = new MockTransaction() {
                @Override
                public FutureResponse<ResultSet> executeDump(
                        PreparedStatement statement,
                        Collection<? extends SqlRequest.Parameter> parameters,
                        Path directory,
                        SqlRequest.DumpOption option) throws IOException {
                    return FutureResponse.raises(new EvaluationException(SqlServiceCode.EVALUATION_EXCEPTION));
                }
            };
        ) {
            operation.register(client, monitor, target("T1"));
            var e = assertThrows(DiagnosticException.class, () -> operation.execute(client, tx, monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.OPERATION_FAILURE, e.getDiagnosticCode());
        }
    }

    @Test
    void execute_operation_failure_io_error() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient();
            var tx = new MockTransaction() {
                @Override
                public FutureResponse<ResultSet> executeDump(
                        PreparedStatement statement,
                        Collection<? extends SqlRequest.Parameter> parameters,
                        Path directory,
                        SqlRequest.DumpOption option) throws IOException {
                    throw new IOException();
                }
            };
        ) {
            operation.register(client, monitor, target("T1"));
            var e = assertThrows(DiagnosticException.class, () -> operation.execute(client, tx, monitor, target("T1")));
            assertEquals(DumpDiagnosticCode.IO_ERROR, e.getDiagnosticCode());
        }
    }

    @Test
    void execute_without_register() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient();
            var tx = new MockTransaction();
        ) {
            operation.register(client, monitor, target("T1"));
            assertThrows(IllegalArgumentException.class, () -> operation.execute(client, tx, monitor, target("XXX")));
        }
    }

    @Test
    void execute_unsupported_target_type() throws Exception {
        var operation = createOperation();
        try (
            var client = new MockSqlClient();
            var tx = new MockTransaction();
        ) {
            var t1 = new DumpTarget(DumpTarget.TargetType.QUERY, "mismatch", "mismatch", destination("mismatch"));
            var t2 = new DumpTarget(DumpTarget.TargetType.TABLE, "mismatch", "mismatch", destination("mismatch"));
            operation.register(client, monitor, t1);
            assertThrows(UnsupportedOperationException.class, () -> operation.execute(client, tx, monitor, t2));
        }
    }
}
