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
package com.tsurugidb.tgsql.core.executor.sql;

import java.util.Optional;
import java.util.stream.Collectors;

import com.tsurugidb.sql.proto.SqlCommon.AtomType;
import com.tsurugidb.sql.proto.SqlCommon.Column;

/**
 * Column.
 */
public class ColumnWrapper {

    private final Column column;

    public ColumnWrapper(Column column) {
        this.column = column;
    }

    public String getName() {
        return column.getName();
    }

    public int getDimension() {
        return column.getDimension();
    }

    /**
     * get type.
     *
     * @return type text
     */
    public String getTypeText() {
        switch (column.getTypeInfoCase()) {
        case ATOM_TYPE:
            return getTypeTextForAtomType();
        case ROW_TYPE:
            return column.getRowType().getColumnsList().stream().map(c -> new ColumnWrapper(c).getTypeText()).collect(Collectors.joining(", ", "[", "]"));
        case USER_TYPE:
            return column.getUserType().getName();
        case TYPEINFO_NOT_SET:
        default:
            return "";
        }
    }

    protected String getTypeTextForAtomType() {
        var atomType = column.getAtomType();
        switch (atomType) {
        case INT4:
            return "INT";
        case INT8:
            return "BIGINT";
        case FLOAT4:
            return "REAL";
        case FLOAT8:
            return "DOUBLE";
        case DECIMAL:
            return getSqlTypeDecimal();
        case CHARACTER:
            return getSqlTypeVarLength(atomType, "CHAR");
        case OCTET:
            return getSqlTypeVarLength(atomType, "BINARY");
        case DATE:
            return "DATE";
        case TIME_OF_DAY:
            return "TIME";
        case TIME_OF_DAY_WITH_TIME_ZONE:
            return "TIME WITH TIME ZONE";
        case TIME_POINT:
            return "TIMESTAMP";
        case TIME_POINT_WITH_TIME_ZONE:
            return "TIMESTAMP WITH TIME ZONE";
        default:
            return atomType.name();
        }
    }

    /**
     * Get SQL type (DECIMAL).
     *
     * @return SQL type
     */
    protected String getSqlTypeDecimal() {
        var sb = new StringBuilder("DECIMAL");
        findPrecision().ifPresent(precision -> {
            sb.append('(');
            sb.append(precision);

            findScale().ifPresent(scale -> {
                sb.append(", ");
                sb.append(scale);
            });
            sb.append(')');
        });
        return sb.toString();
    }

    /**
     * Get SQL type (VAR(length)).
     *
     * @param atomType AtomType
     * @param baseName base name
     * @return SQL type
     */
    protected String getSqlTypeVarLength(AtomType atomType, String baseName) {
        var varying = findVarying();
        if (varying.isEmpty()) {
            return atomType.name();
        }

        var sb = new StringBuilder();
        if (varying.get()) {
            sb.append("VAR");
        }
        sb.append(baseName);

        findLength().ifPresent(length -> {
            sb.append('(');
            sb.append(length);
            sb.append(')');
        });
        return sb.toString();
    }

    /**
     * Get constraint.
     *
     * @return constraint
     */
    public String getConstraintText() {
        var sb = new StringBuilder();
        findNullable().ifPresent(nullable -> {
            appendConstraint(sb, nullable ? "NULL" : "NOT NULL");
        });
        return sb.toString();
    }

    protected void appendConstraint(StringBuilder sb, String s) {
        if (sb.length() != 0) {
            sb.append(" ");
        }
        sb.append(s);
    }

    /**
     * Get length for data types.
     *
     * @return length
     */
    public Optional<ArbitraryInt> findLength() {
        var c = column.getLengthOptCase();
        if (c == null) {
            return Optional.empty();
        }
        switch (c) {
        case LENGTH:
            return Optional.of(ArbitraryInt.of(column.getLength()));
        case ARBITRARY_LENGTH:
            return Optional.of(ArbitraryInt.ofArbitrary());
        case LENGTHOPT_NOT_SET:
        default:
            return Optional.empty();
        }
    }

    /**
     * Get precision for decimal types.
     *
     * @return precision
     */
    public Optional<ArbitraryInt> findPrecision() {
        var c = column.getPrecisionOptCase();
        if (c == null) {
            return Optional.empty();
        }
        switch (c) {
        case PRECISION:
            return Optional.of(ArbitraryInt.of(column.getPrecision()));
        case ARBITRARY_PRECISION:
            return Optional.of(ArbitraryInt.ofArbitrary());
        case PRECISIONOPT_NOT_SET:
        default:
            return Optional.empty();
        }
    }

    /**
     * Get scale for decimal types.
     *
     * @return scale
     */
    public Optional<ArbitraryInt> findScale() {
        var c = column.getScaleOptCase();
        if (c == null) {
            return Optional.empty();
        }
        switch (c) {
        case SCALE:
            return Optional.of(ArbitraryInt.of(column.getScale()));
        case ARBITRARY_SCALE:
            return Optional.of(ArbitraryInt.ofArbitrary());
        case SCALEOPT_NOT_SET:
        default:
            return Optional.empty();
        }
    }

    /**
     * Whether the column type is nullable.
     *
     * @return nullable
     */
    public Optional<Boolean> findNullable() {
        var c = column.getNullableOptCase();
        if (c == null) {
            return Optional.empty();
        }
        switch (c) {
        case NULLABLE:
            return Optional.of(column.getNullable());
        case NULLABLEOPT_NOT_SET:
        default:
            return Optional.empty();
        }
    }

    /**
     * Whether the column type is varying.
     *
     * @return varying
     */
    public Optional<Boolean> findVarying() {
        var c = column.getVaryingOptCase();
        if (c == null) {
            return Optional.empty();
        }
        switch (c) {
        case VARYING:
            return Optional.of(column.getVarying());
        case VARYINGOPT_NOT_SET:
        default:
            return Optional.empty();
        }
    }

    public Optional<String> findDescription() {
        var c = column.getDescriptionOptCase();
        if (c == null) {
            return Optional.empty();
        }
        switch (c) {
        case DESCRIPTION:
            return Optional.of(column.getDescription());
        case DESCRIPTIONOPT_NOT_SET:
        default:
            return Optional.empty();
        }
    }
}
