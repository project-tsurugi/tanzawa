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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.sql.PreparedStatement;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.util.FutureResponse;

class MockTransaction implements Transaction {

    @Override
    public FutureResponse<ResultSet> executeDump(
            PreparedStatement statement,
            Collection<? extends SqlRequest.Parameter> parameters,
            Path directory,
            SqlRequest.DumpOption option) throws IOException {
        return FutureResponse.returns(new MockResultSet(List.of(BasicDumpSessionTest.dumpFile(directory, 1).toString())));
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