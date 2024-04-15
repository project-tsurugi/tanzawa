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
