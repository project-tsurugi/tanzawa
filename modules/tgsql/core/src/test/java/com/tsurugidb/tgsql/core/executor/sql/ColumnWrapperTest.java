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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.google.protobuf.Empty;
import com.tsurugidb.sql.proto.SqlCommon.AtomType;
import com.tsurugidb.sql.proto.SqlCommon.Column;

class ColumnWrapperTest {

    @Test
    void getName() {
        var c = Column.newBuilder().setName("test").build();
        var column = new ColumnWrapper(c);

        assertEquals("test", column.getName());
    }

    @Test
    void getTypeText_INT4() {
        var c = Column.newBuilder().setAtomType(AtomType.INT4).build();
        var column = new ColumnWrapper(c);

        assertEquals("INT", column.getTypeText());
    }

    @Test
    void getTypeText_INT8() {
        var c = Column.newBuilder().setAtomType(AtomType.INT8).build();
        var column = new ColumnWrapper(c);

        assertEquals("BIGINT", column.getTypeText());
    }

    @Test
    void getTypeText_FLOAT4() {
        var c = Column.newBuilder().setAtomType(AtomType.FLOAT4).build();
        var column = new ColumnWrapper(c);

        assertEquals("REAL", column.getTypeText());
    }

    @Test
    void getTypeText_FLOAT8() {
        var c = Column.newBuilder().setAtomType(AtomType.FLOAT8).build();
        var column = new ColumnWrapper(c);

        assertEquals("DOUBLE", column.getTypeText());
    }

    @Test
    void getTypeText_DECIMAL() {
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.DECIMAL).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("DECIMAL", column.getTypeText());
        }

        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.DECIMAL).setPrecision(123).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("DECIMAL(123)", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.DECIMAL).setPrecision(123).setScale(4).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("DECIMAL(123, 4)", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.DECIMAL).setPrecision(123).setArbitraryScale(Empty.newBuilder().build()).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("DECIMAL(123, *)", column.getTypeText());
        }

        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.DECIMAL).setArbitraryPrecision(Empty.newBuilder().build()).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("DECIMAL(*)", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.DECIMAL).setArbitraryPrecision(Empty.newBuilder().build()).setScale(123).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("DECIMAL(*, 123)", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.DECIMAL).setArbitraryPrecision(Empty.newBuilder().build()).setArbitraryScale(Empty.newBuilder().build()).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("DECIMAL(*, *)", column.getTypeText());
        }
    }

    @Test
    void getTypeText_CHARACTER() {
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.CHARACTER).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("CHARACTER", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.CHARACTER).setLength(123).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("CHARACTER", column.getTypeText());
        }

        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.CHARACTER).setVarying(false).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("CHAR", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.CHARACTER).setVarying(false).setLength(123).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("CHAR(123)", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.CHARACTER).setVarying(false).setArbitraryLength(Empty.newBuilder().build()).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("CHAR(*)", column.getTypeText());
        }

        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.CHARACTER).setVarying(true).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("VARCHAR", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.CHARACTER).setVarying(true).setLength(123).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("VARCHAR(123)", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.CHARACTER).setVarying(true).setArbitraryLength(Empty.newBuilder().build()).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("VARCHAR(*)", column.getTypeText());
        }
    }

    @Test
    void getTypeText_OCTET() {
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.OCTET).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("OCTET", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.OCTET).setLength(123).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("OCTET", column.getTypeText());
        }

        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.OCTET).setVarying(false).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("BINARY", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.OCTET).setVarying(false).setLength(123).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("BINARY(123)", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.OCTET).setVarying(false).setArbitraryLength(Empty.newBuilder().build()).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("BINARY(*)", column.getTypeText());
        }

        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.OCTET).setVarying(true).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("VARBINARY", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.OCTET).setVarying(true).setLength(123).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("VARBINARY(123)", column.getTypeText());
        }
        {
            var lowColumn = Column.newBuilder().setAtomType(AtomType.OCTET).setVarying(true).setArbitraryLength(Empty.newBuilder().build()).build();
            var column = new ColumnWrapper(lowColumn);

            assertEquals("VARBINARY(*)", column.getTypeText());
        }
    }

    @Test
    void getTypeText_DATE() {
        var lowColumn = Column.newBuilder().setAtomType(AtomType.DATE).build();
        var column = new ColumnWrapper(lowColumn);

        assertEquals("DATE", column.getTypeText());
    }

    @Test
    void getTypeText_TIME() {
        var lowColumn = Column.newBuilder().setAtomType(AtomType.TIME_OF_DAY).build();
        var column = new ColumnWrapper(lowColumn);

        assertEquals("TIME", column.getTypeText());
    }

    @Test
    void getTypeText_TIME_TIMEZONE() {
        var lowColumn = Column.newBuilder().setAtomType(AtomType.TIME_OF_DAY_WITH_TIME_ZONE).build();
        var column = new ColumnWrapper(lowColumn);

        assertEquals("TIME WITH TIME ZONE", column.getTypeText());
    }

    @Test
    void getTypeText_TIMEPOINT() {
        var lowColumn = Column.newBuilder().setAtomType(AtomType.TIME_POINT).build();
        var column = new ColumnWrapper(lowColumn);

        assertEquals("TIMESTAMP", column.getTypeText());
    }

    @Test
    void getTypeText_TIMEPOINT_TIMEZONE() {
        var lowColumn = Column.newBuilder().setAtomType(AtomType.TIME_POINT_WITH_TIME_ZONE).build();
        var column = new ColumnWrapper(lowColumn);

        assertEquals("TIMESTAMP WITH TIME ZONE", column.getTypeText());
    }

    @Test
    void getTypeText_BLOB() {
        var lowColumn = Column.newBuilder().setAtomType(AtomType.BLOB).build();
        var column = new ColumnWrapper(lowColumn);

        assertEquals("BLOB", column.getTypeText());
    }

    @Test
    void getTypeText_CLOB() {
        var lowColumn = Column.newBuilder().setAtomType(AtomType.CLOB).build();
        var column = new ColumnWrapper(lowColumn);

        assertEquals("CLOB", column.getTypeText());
    }
}
