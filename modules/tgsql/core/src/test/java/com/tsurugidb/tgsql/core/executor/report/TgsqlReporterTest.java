package com.tsurugidb.tgsql.core.executor.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tsubakuro.sql.CounterType;

class TgsqlReporterTest {

    private static final TgsqlReporter REPORTER = new TgsqlReporter() {

        @Override
        public void warn(String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void succeed(String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void info(String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void implicit(String message) {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    void getStatementResultMessage() {
        {
            var counterType = CounterType.INSERTED_ROWS;
            assertEquals("0 rows inserted", REPORTER.getStatementResultMessage(counterType, 0));
            assertEquals("1 row inserted", REPORTER.getStatementResultMessage(counterType, 1));
            assertEquals("2 rows inserted", REPORTER.getStatementResultMessage(counterType, 2));
        }
        {
            var counterType = CounterType.UPDATED_ROWS;
            assertEquals("0 rows updated", REPORTER.getStatementResultMessage(counterType, 0));
            assertEquals("1 row updated", REPORTER.getStatementResultMessage(counterType, 1));
            assertEquals("2 rows updated", REPORTER.getStatementResultMessage(counterType, 2));
        }
        {
            var counterType = CounterType.MERGED_ROWS;
            assertEquals("0 rows merged", REPORTER.getStatementResultMessage(counterType, 0));
            assertEquals("1 row merged", REPORTER.getStatementResultMessage(counterType, 1));
            assertEquals("2 rows merged", REPORTER.getStatementResultMessage(counterType, 2));
        }
        {
            var counterType = CounterType.DELETED_ROWS;
            assertEquals("0 rows deleted", REPORTER.getStatementResultMessage(counterType, 0));
            assertEquals("1 row deleted", REPORTER.getStatementResultMessage(counterType, 1));
            assertEquals("2 rows deleted", REPORTER.getStatementResultMessage(counterType, 2));
        }
    }
}
