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
import java.util.List;

import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.ResultSet;
import com.tsurugidb.tsubakuro.sql.ResultSetMetadata;
import com.tsurugidb.tsubakuro.sql.Types;
import com.tsurugidb.tsubakuro.sql.impl.EmptyRelationCursor;

class MockResultSet extends EmptyRelationCursor implements ResultSet {

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