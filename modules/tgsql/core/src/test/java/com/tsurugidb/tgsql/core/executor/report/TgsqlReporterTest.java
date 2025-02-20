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
