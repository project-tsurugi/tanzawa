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

import com.tsurugidb.sql.proto.SqlRequest.ReadArea;
import com.tsurugidb.sql.proto.SqlRequest.TransactionOption;
import com.tsurugidb.sql.proto.SqlRequest.TransactionPriority;
import com.tsurugidb.sql.proto.SqlRequest.TransactionType;
import com.tsurugidb.sql.proto.SqlRequest.WritePreserve;

class TransactionOptionReportUtilTest {

    private static final TransactionOptionReportUtil TARGET = TransactionOptionReportUtil.getInstance();

    @Test
    void toStringOcc() {
        {
            var option = TransactionOption.newBuilder().setType(TransactionType.SHORT).build();
            var actual = TARGET.toString(option);
            var expected = "[" //
                    + "\n  type: OCC" //
                    + "\n]";
            assertEquals(expected, actual);
        }
        {
            var option = TransactionOption.newBuilder().setType(TransactionType.SHORT).setLabel("test").build();
            var actual = TARGET.toString(option);
            var expected = "[" //
                    + "\n  type: OCC" //
                    + "\n  label: \"test\"" //
                    + "\n]";
            assertEquals(expected, actual);
        }
    }

    @Test
    void toStringLtx() {
        {
            var option = TransactionOption.newBuilder().setType(TransactionType.LONG).setModifiesDefinitions(true).build();
            var actual = TARGET.toString(option);
            var expected = "[" //
                    + "\n  type: LTX" //
                    + "\n  include_ddl: true" //
                    + "\n]";
            assertEquals(expected, actual);
        }
        {
            var option = TransactionOption.newBuilder().setType(TransactionType.LONG).addWritePreserves(WritePreserve.newBuilder().setTableName("test")).build();
            var actual = TARGET.toString(option);
            var expected = "[" //
                    + "\n  type: LTX" //
                    + "\n  write_preserve: \"test\"" //
                    + "\n]";
            assertEquals(expected, actual);
        }
        {
            var option = TransactionOption.newBuilder().setType(TransactionType.LONG) //
                    .addWritePreserves(WritePreserve.newBuilder().setTableName("test1")) //
                    .addWritePreserves(WritePreserve.newBuilder().setTableName("test2")) //
                    .build();
            var actual = TARGET.toString(option);
            var expected = "[" //
                    + "\n  type: LTX" //
                    + "\n  write_preserve: \"test1\", \"test2\"" //
                    + "\n]";
            assertEquals(expected, actual);
        }
        {
            var option = TransactionOption.newBuilder().setType(TransactionType.LONG) //
                    .addWritePreserves(WritePreserve.newBuilder().setTableName("test1")) //
                    .addWritePreserves(WritePreserve.newBuilder().setTableName("test2")) //
                    .addInclusiveReadAreas(ReadArea.newBuilder().setTableName("in1")) //
                    .addInclusiveReadAreas(ReadArea.newBuilder().setTableName("in2")) //
                    .addExclusiveReadAreas(ReadArea.newBuilder().setTableName("ex1")) //
                    .addExclusiveReadAreas(ReadArea.newBuilder().setTableName("ex2")) //
                    .setPriority(TransactionPriority.WAIT) //
                    .build();
            var actual = TARGET.toString(option);
            var expected = "[" //
                    + "\n  type: LTX" //
                    + "\n  write_preserve: \"test1\", \"test2\"" //
                    + "\n  read_area_include: \"in1\", \"in2\"" //
                    + "\n  read_area_exclude: \"ex1\", \"ex2\"" //
                    + "\n  priority: prior deferrable" //
                    + "\n]";
            assertEquals(expected, actual);
        }
    }

    @Test
    void toStringRtx() {
        {
            var option = TransactionOption.newBuilder().setType(TransactionType.READ_ONLY).build();
            var actual = TARGET.toString(option);
            var expected = "[" //
                    + "\n  type: RTX" //
                    + "\n]";
            assertEquals(expected, actual);
        }
    }
}
