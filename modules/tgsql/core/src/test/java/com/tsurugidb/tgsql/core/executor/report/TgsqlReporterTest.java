package com.tsurugidb.tgsql.core.executor.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tsubakuro.sql.CounterType;

class TgsqlReporterTest {

    @Test
    void getStatementResultMessage() {
        var reporter = new TestReporter();
        {
            var counterType = CounterType.INSERTED_ROWS;
            assertEquals("0 rows inserted", reporter.getStatementResultMessage(counterType, 0));
            assertEquals("1 row inserted", reporter.getStatementResultMessage(counterType, 1));
            assertEquals("2 rows inserted", reporter.getStatementResultMessage(counterType, 2));
        }
        {
            var counterType = CounterType.UPDATED_ROWS;
            assertEquals("0 rows updated", reporter.getStatementResultMessage(counterType, 0));
            assertEquals("1 row updated", reporter.getStatementResultMessage(counterType, 1));
            assertEquals("2 rows updated", reporter.getStatementResultMessage(counterType, 2));
        }
        {
            var counterType = CounterType.MERGED_ROWS;
            assertEquals("0 rows merged", reporter.getStatementResultMessage(counterType, 0));
            assertEquals("1 row merged", reporter.getStatementResultMessage(counterType, 1));
            assertEquals("2 rows merged", reporter.getStatementResultMessage(counterType, 2));
        }
        {
            var counterType = CounterType.DELETED_ROWS;
            assertEquals("0 rows deleted", reporter.getStatementResultMessage(counterType, 0));
            assertEquals("1 row deleted", reporter.getStatementResultMessage(counterType, 1));
            assertEquals("2 rows deleted", reporter.getStatementResultMessage(counterType, 2));
        }
    }
}
