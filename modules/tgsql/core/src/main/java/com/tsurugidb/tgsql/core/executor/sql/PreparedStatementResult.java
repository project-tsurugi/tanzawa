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
package com.tsurugidb.tgsql.core.executor.sql;

import java.io.IOException;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.ExecuteResult;
import com.tsurugidb.tsubakuro.sql.ResultSet;

public class PreparedStatementResult implements AutoCloseable {

    private final ResultSet resultSet;
    private final ExecuteResult executeResult;

    public PreparedStatementResult(ResultSet resultSet) {
        this.resultSet = resultSet;
        this.executeResult = null;
    }

    public PreparedStatementResult(ExecuteResult executeResult) {
        this.resultSet = null;
        this.executeResult = executeResult;
    }

    public ResultSet getResultSet() {
        return this.resultSet;
    }

    public ExecuteResult getExecuteResult() {
        return this.executeResult;
    }

    @Override
    public void close() throws ServerException, IOException, InterruptedException {
        try (resultSet) {
            // close only
        }
    }
}
