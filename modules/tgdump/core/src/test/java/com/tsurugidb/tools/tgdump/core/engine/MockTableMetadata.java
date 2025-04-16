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

import java.util.List;
import java.util.Optional;

import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tsubakuro.sql.TableMetadata;

/**
 * Mock implementation of {@link TableMetadata}.
 */
public class MockTableMetadata implements TableMetadata {

    private final String tableName;

    /**
     * Creates a new instance.
     *
     * @param tableName the table name
     */
    public MockTableMetadata(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public Optional<String> getDatabaseName() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getSchemaName() {
        return Optional.empty();
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.empty();
    }

    @Override
    public List<? extends SqlCommon.Column> getColumns() {
        return List.of(SqlCommon.Column.newBuilder() //
                .setName("k") //
                .setAtomType(SqlCommon.AtomType.INT4) //
                .build());
    }
}
