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
package com.tsurugidb.tgsql.core.executor.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlResponse;
import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.config.TgsqlCvKey;
import com.tsurugidb.tgsql.core.exception.TgsqlNoMessageException;
import com.tsurugidb.tgsql.core.executor.explain.StatementMetadataHandler;
import com.tsurugidb.tgsql.core.executor.report.TestReporter;
import com.tsurugidb.tgsql.core.executor.result.ResultProcessor;
import com.tsurugidb.tgsql.core.executor.sql.PreparedStatementResult;
import com.tsurugidb.tgsql.core.executor.sql.SqlProcessor;
import com.tsurugidb.tgsql.core.executor.sql.TransactionWrapper;
import com.tsurugidb.tgsql.core.model.Region;
import com.tsurugidb.tgsql.core.model.Statement;
import com.tsurugidb.tgsql.core.parser.SqlParser;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.explain.PlanGraph;
import com.tsurugidb.tsubakuro.explain.PlanNode;
import com.tsurugidb.tsubakuro.explain.json.JsonPlanGraphLoader;
import com.tsurugidb.tsubakuro.sql.CounterType;
import com.tsurugidb.tsubakuro.sql.ExecuteResult;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.SqlServiceException;
import com.tsurugidb.tsubakuro.sql.StatementMetadata;
import com.tsurugidb.tsubakuro.sql.TableMetadata;
import com.tsurugidb.tsubakuro.sql.Types;
import com.tsurugidb.tsubakuro.sql.impl.BasicStatementMetadata;
import com.tsurugidb.tsubakuro.sql.impl.ResultSetMetadataAdapter;
import com.tsurugidb.tsubakuro.sql.impl.testing.Relation;

class BasicEngineTest {

    static class MockSqlProcessor implements SqlProcessor {

        private boolean active = false;
        private String transactionId = null;
        private TransactionWrapper transaction = null;

        MockSqlProcessor() {
            this(false);
        }

        MockSqlProcessor(boolean active) {
            this.active = active;
            this.transactionId = active ? "MockTx" : null;
        }

        @Override
        public void connect(TgsqlConfig config) throws ServerException, IOException, InterruptedException {
            return;
        }

        @Override
        public boolean disconnect() throws ServerException, IOException, InterruptedException {
            return true;
        }

        @Override
        public List<String> getTableNames() throws ServerException, IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public TableMetadata getTableMetadata(String tableName) throws ServerException, IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isSessionActive() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TransactionWrapper getTransaction() {
            if (this.transaction == null) {
                this.transaction = new TransactionWrapper(null, null);
            }
            return this.transaction;
        }

        @Override
        public boolean isTransactionActive() {
            return active;
        }

        @Override
        public String getTransactionId() {
            return transactionId;
        }

        @Override
        public SqlServiceException getTransactionException() throws ServerException, IOException, InterruptedException {
            return null;
        }

        @Override
        public PreparedStatementResult execute(String statement, Region region) throws ServerException, IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void commitTransaction(SqlRequest.CommitStatus status) throws ServerException, IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void rollbackTransaction() throws ServerException, IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public StatementMetadata explain(String statement, Region region) throws ServerException, IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }
    }

    static class MockResultProcessor implements ResultProcessor {

        @Override
        public long process(TransactionWrapper transaction, ResultSet target) throws ServerException, IOException, InterruptedException {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void empty_statement() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor();
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse(";"));
        assertTrue(cont);
    }

    @Test
    void generic_statement_wo_result() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(true) {
            @Override
            public PreparedStatementResult execute(String statement, Region region) {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals("INSERT INTO A DEFAULT VALUES", statement);
                var er = new ExecuteResult() {
                    @Override
                    public Map<CounterType, Long> getCounters() {
                        return Map.of(CounterType.INSERTED_ROWS, 1L);
                    }
                };
                return new PreparedStatementResult(er);
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("INSERT INTO A DEFAULT VALUES"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void generic_statement_w_result() throws Exception {
        var reachedExec = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(true) {
            @Override
            public PreparedStatementResult execute(String statement, Region region) {
                if (!reachedExec.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals("SELECT * FROM T", statement);
                var rs = Relation.of(new Object[][] { { 1 } }).getResultSet(new ResultSetMetadataAdapter(SqlResponse.ResultSetMetadata.newBuilder().addColumns(Types.column(int.class)).build()));
                return new PreparedStatementResult(rs, null);
            }
        };
        var reachedRs = new AtomicBoolean();
        MockResultProcessor rs = new MockResultProcessor() {
            @Override
            public long process(TransactionWrapper transaction, ResultSet target) throws ServerException, IOException, InterruptedException {
                if (!reachedRs.compareAndSet(false, true)) {
                    fail();
                }
                assertTrue(target.nextRow());
                assertTrue(target.nextColumn());
                assertEquals(1, target.fetchInt4Value());
                assertFalse(target.nextColumn());
                assertFalse(target.nextRow());
                return System.nanoTime();
            }
        };
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("SELECT * FROM T"));
        assertTrue(cont);
        assertTrue(reachedExec.get());
        assertTrue(reachedRs.get());
    }

    @Test
    void call_statement_fall_through() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(true) {
            @Override
            public PreparedStatementResult execute(String statement, Region region) {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals("CALL proc()", statement);
                var er = new ExecuteResult() {
                    @Override
                    public Map<CounterType, Long> getCounters() {
                        return Map.of();
                    }
                };
                return new PreparedStatementResult(er);
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("CALL proc()"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void generic_statement_inactive_tx() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        assertThrows(EngineException.class, () -> engine.execute(parse("SELECT * FROM T")));
    }

    @Test
    void start_transaction_statement() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.SHORT, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_long() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.LONG, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START LONG TRANSACTION"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_prior() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.SHORT, option.getType());
                assertEquals(SqlRequest.TransactionPriority.WAIT, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION EXECUTE PRIOR"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_as() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.SHORT, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("TESTING", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION AS TESTING"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_as_time() throws Exception {
        String format = "yyyyMMdd HH:mm:ss.SSSSSS";
        var formatter = DateTimeFormatter.ofPattern(format);

        var reached = new AtomicBoolean();
        var start = "TESTING" + ZonedDateTime.now().format(formatter);
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.SHORT, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                var end = "TESTING" + ZonedDateTime.now().format(formatter);
                String label = option.getLabel();
                if (start.compareTo(label) <= 0 && label.compareTo(end) <= 0) {
                    // success
                } else {
                    fail(String.format("label fail. label=[%s], start=[%s], end=[%s]", label, start, end));
                }
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        engine.getConfig().getClientVariableMap().put(TgsqlCvKey.TX_LABEL_SUFFIX_TIME, format);
        var cont = engine.execute(parse("START TRANSACTION AS TESTING"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_write_preserve() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.LONG, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(3, option.getWritePreservesCount());
                assertEquals("a", option.getWritePreserves(0).getTableName());
                assertEquals("b", option.getWritePreserves(1).getTableName());
                assertEquals("c", option.getWritePreserves(2).getTableName());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION WRITE PRESERVE a, b, c"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @ParameterizedTest
    @ValueSource(strings = { "DEFINITION", "DEFINITIONS", "DDL" })
    void start_transaction_statement_include_ddl(String word) throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.LONG, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertTrue(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START LONG TRANSACTION INCLUDE " + word));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @ParameterizedTest
    @ValueSource(strings = { "DEFERRABLE", "IMMEDIATE", "" })
    void start_transaction_statement_include_ddl_read_only(String word) throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var e = assertThrows(EngineException.class, () -> engine.execute(parse("START TRANSACTION INCLUDE DDL READ ONLY " + word)));
        assertEquals("include ddl is conflicted \"READ ONLY\"", e.getMessage());
    }

    @Test
    void start_transaction_statement_include_ddl_shortTx() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.SHORT, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION INCLUDE DDL"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_read_only() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.READ_ONLY, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION READ ONLY"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_read_only_immediate() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.LONG, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION READ ONLY IMMEDIATE"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_read_only_deferrable() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.READ_ONLY, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION READ ONLY DEFERRABLE"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_read_only_write_preserve() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        assertThrows(EngineException.class, () -> engine.execute(parse("START TRANSACTION READ ONLY WRITE PRESERVE t1")));
    }

    @Test
    void start_transaction_statement_long_read_only() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        assertThrows(EngineException.class, () -> engine.execute(parse("START LONG TRANSACTION READ ONLY")));
    }

    @Test
    void start_transaction_statement_read_area_include() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.LONG, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(3, option.getInclusiveReadAreasCount());
                assertEquals("a", option.getInclusiveReadAreas(0).getTableName());
                assertEquals("b", option.getInclusiveReadAreas(1).getTableName());
                assertEquals("c", option.getInclusiveReadAreas(2).getTableName());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION READ AREA INCLUDE a, b, c"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_read_area_exclude() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.LONG, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(3, option.getExclusiveReadAreasCount());
                assertEquals("a", option.getExclusiveReadAreas(0).getTableName());
                assertEquals("b", option.getExclusiveReadAreas(1).getTableName());
                assertEquals("c", option.getExclusiveReadAreas(2).getTableName());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION READ AREA EXCLUDE a, b, c"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_read_area_both() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.LONG, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(3, option.getInclusiveReadAreasCount());
                assertEquals("a", option.getInclusiveReadAreas(0).getTableName());
                assertEquals("b", option.getInclusiveReadAreas(1).getTableName());
                assertEquals("c", option.getInclusiveReadAreas(2).getTableName());
                assertEquals(3, option.getExclusiveReadAreasCount());
                assertEquals("d", option.getExclusiveReadAreas(0).getTableName());
                assertEquals("e", option.getExclusiveReadAreas(1).getTableName());
                assertEquals("f", option.getExclusiveReadAreas(2).getTableName());
                assertEquals(0, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION READ AREA INCLUDE a, b, c EXCLUDE d, e, f"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_parallel() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(false) {
            @Override
            public void startTransaction(SqlRequest.TransactionOption option) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.TransactionType.READ_ONLY, option.getType());
                assertEquals(SqlRequest.TransactionPriority.TRANSACTION_PRIORITY_UNSPECIFIED, option.getPriority());
                assertEquals("", option.getLabel());
                assertEquals(0, option.getWritePreservesCount());
                assertFalse(option.getModifiesDefinitions());
                assertEquals(0, option.getInclusiveReadAreasCount());
                assertEquals(0, option.getExclusiveReadAreasCount());
                assertEquals(123, option.getScanParallel());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("START TRANSACTION READ ONLY WITH PARALLEL=123"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void start_transaction_statement_tx_active() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(true);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        assertThrows(EngineException.class, () -> engine.execute(parse("START TRANSACTION")));
    }

    @Test
    void commit_statement() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(true) {
            @Override
            public void commitTransaction(SqlRequest.CommitStatus status) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(null, status);
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("COMMIT"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void commit_statement_stored() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(true) {
            @Override
            public void commitTransaction(SqlRequest.CommitStatus status) throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
                assertEquals(SqlRequest.CommitStatus.STORED, status);
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("COMMIT WAIT STORED"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void commit_statement_tx_inactive() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        assertThrows(EngineException.class, () -> engine.execute(parse("COMMIT")));
    }

    @Test
    void rollback_statement() throws Exception {
        var reached = new AtomicBoolean();
        MockSqlProcessor sql = new MockSqlProcessor(true) {
            @Override
            public void rollbackTransaction() throws ServerException, IOException, InterruptedException {
                if (!reached.compareAndSet(false, true)) {
                    fail();
                }
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("ROLLBACK"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void rollback_statement_tx_inactive() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        assertThrows(EngineException.class, () -> engine.execute(parse("COMMIT")));
    }

    @Test
    void explain_statement() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(true) {
            @Override
            public StatementMetadata explain(String statement, Region region) throws IOException {
                assertEquals("SELECT 1", statement);
                return new BasicStatementMetadata(JsonPlanGraphLoader.SUPPORTED_FORMAT_ID, 1, // captured version of the explain result
                        TestUtil.read("explain-find-project-write.json"), List.of());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var reached = new AtomicBoolean();
        var reporter = new TestReporter() {
            @Override
            public void reportExecutionPlan(String source, PlanGraph plan) {
                reached.set(true);
                assertEquals("SELECT 1", source);
                assertTrue(plan.getNodes().stream().map(PlanNode::getKind).anyMatch(Predicate.isEqual("find")));
                assertFalse(plan.getNodes().stream().map(PlanNode::getKind).anyMatch(Predicate.isEqual("project")));
                assertTrue(plan.getNodes().stream().map(PlanNode::getKind).anyMatch(Predicate.isEqual("write")));
            }
        };
        var engine = new BasicEngine(new TgsqlConfig(), sql, rs, reporter);
        var cont = engine.execute(parse("EXPLAIN SELECT 1"));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void explain_statement_option() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(true) {
            @Override
            public StatementMetadata explain(String statement, Region region) throws IOException {
                assertEquals("SELECT 1", statement);
                return new BasicStatementMetadata(JsonPlanGraphLoader.SUPPORTED_FORMAT_ID, 1, // captured version of the explain result
                        TestUtil.read("explain-find-project-write.json"), List.of());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var reached = new AtomicBoolean();
        var reporter = new TestReporter() {
            @Override
            public void reportExecutionPlan(String source, PlanGraph plan) {
                reached.set(true);
                assertEquals("SELECT 1", source);
                assertTrue(plan.getNodes().stream().map(PlanNode::getKind).anyMatch(Predicate.isEqual("find")));
                assertTrue(plan.getNodes().stream().map(PlanNode::getKind).anyMatch(Predicate.isEqual("project")));
                assertTrue(plan.getNodes().stream().map(PlanNode::getKind).anyMatch(Predicate.isEqual("write")));
            }
        };
        var engine = new BasicEngine(new TgsqlConfig(), sql, rs, reporter);
        var cont = engine.execute(parse(String.format("EXPLAIN (%s) SELECT 1", StatementMetadataHandler.KEY_VERBOSE)));
        assertTrue(cont);
        assertTrue(reached.get());
    }

    @Test
    void explain_statement_option_unknown() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(true);
        MockResultProcessor rs = new MockResultProcessor();
        var reporter = new TestReporter();
        var engine = new BasicEngine(new TgsqlConfig(), sql, rs, reporter);
        assertThrows(EngineException.class, () -> engine.execute(parse("EXPLAIN (INVALID_OPTION=TRUE) SELECT 1")));
    }

    @Test
    void explain_statement_option_invalid() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(true);
        MockResultProcessor rs = new MockResultProcessor();
        var reporter = new TestReporter();
        var engine = new BasicEngine(new TgsqlConfig(), sql, rs, reporter);
        assertThrows(EngineException.class, () -> engine.execute(parse(String.format("EXPLAIN (%s=NULL) SELECT 1", StatementMetadataHandler.KEY_VERBOSE))));
    }

    @Test
    void explain_statement_unsupported_format() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(true) {
            @Override
            public StatementMetadata explain(String statement, Region region) throws IOException {
                assertEquals("SELECT 1", statement);
                return new BasicStatementMetadata("ERRONEOUS_FORMAT", 0, "BROKEN", List.of());
            }
        };
        MockResultProcessor rs = new MockResultProcessor();
        var reporter = new TestReporter();
        var engine = new BasicEngine(new TgsqlConfig(), sql, rs, reporter);
        var e = assertThrows(TgsqlNoMessageException.class, () -> engine.execute(parse("EXPLAIN SELECT 1")));
        assertInstanceOf(EngineException.class, e.getCause());
    }

    @Test
    void special_statement_exit() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("\\exit"));
        assertFalse(cont);
    }

    @Test
    void special_statement_halt() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("\\halt"));
        assertFalse(cont);
    }

    @Test
    void special_statement_status() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("\\status"));
        assertTrue(cont);
    }

    @Test
    void special_statement_status_tx_active() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(true);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("\\status"));
        assertTrue(cont);
    }

    @Test
    void special_statement_help() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("\\help"));
        assertTrue(cont);
    }

    @Test
    void special_statement_help_command() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("\\help start transaction"));
        assertTrue(cont);
    }

    @Test
    void special_statement_help_alias() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("\\help begin"));
        assertTrue(cont);
    }

    @Test
    void special_statement_help_unknown() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("\\help ===UNKNWON==="));
        assertTrue(cont);
    }

    @Test
    void special_statement_exit_tx_active() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(true);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        assertThrows(EngineException.class, () -> engine.execute(parse("\\exit")));
    }

    @Test
    void special_statement_halt_tx_active() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(true);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        var cont = engine.execute(parse("\\halt"));
        assertFalse(cont);
    }

    @Test
    void special_statement_unknown() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        assertThrows(EngineException.class, () -> engine.execute(parse("\\UNKNOWN_COMMAND")));
    }

    @Test
    void erroneous_statement_unknown() throws Exception {
        MockSqlProcessor sql = new MockSqlProcessor(false);
        MockResultProcessor rs = new MockResultProcessor();
        var engine = newBasicEngine(sql, rs);
        assertThrows(EngineException.class, () -> engine.execute(parse("START TRANSACTION INVALID")));
    }

    private static BasicEngine newBasicEngine(MockSqlProcessor sql, MockResultProcessor rs) {
        var config = new TgsqlConfig();
        var reporter = new TestReporter(config);
        return new BasicEngine(config, sql, rs, reporter);
    }

    private static Statement parse(String text) throws IOException {
        try (var parser = new SqlParser(new StringReader(text))) {
            return parser.next();
        }
    }
}
