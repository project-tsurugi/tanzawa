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
    public List<? extends SqlCommon.Column> getColumns() {
        return List.of(SqlCommon.Column.newBuilder()
                .setName("k")
                .setAtomType(SqlCommon.AtomType.INT4)
                .build());
    }
}
