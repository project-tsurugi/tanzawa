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
package com.tsurugidb.tools.tgdump.core.engine;

import java.io.IOException;
import java.util.Collection;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.TableMetadata;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.util.FutureResponse;

class MockSqlClient implements SqlClient {

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