package com.tsurugidb.console.core.executor.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tsurugidb.sql.proto.SqlRequest.ReadArea;
import com.tsurugidb.sql.proto.SqlRequest.TransactionOption;
import com.tsurugidb.sql.proto.SqlRequest.TransactionPriority;
import com.tsurugidb.sql.proto.SqlRequest.TransactionType;
import com.tsurugidb.sql.proto.SqlRequest.WritePreserve;

class ScriptReporterTest {

    private static final ScriptReporter REPORTER = new ScriptReporter() {

        @Override
        public void warn(String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void succeed(String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void info(String message) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void implicit(String message) {
            throw new UnsupportedOperationException();
        }
    };

    @Test
    void toStringOcc() {
        {
            var option = TransactionOption.newBuilder().setType(TransactionType.SHORT).build();
            var actual = REPORTER.toString(option);
            var expected = "[" //
                    + "\n  type: OCC" //
                    + "\n]";
            assertEquals(expected, actual);
        }
        {
            var option = TransactionOption.newBuilder().setType(TransactionType.SHORT).setLabel("test").build();
            var actual = REPORTER.toString(option);
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
            var actual = REPORTER.toString(option);
            var expected = "[" //
                    + "\n  type: LTX" //
                    + "\n  include_ddl: true" //
                    + "\n]";
            assertEquals(expected, actual);
        }
        {
            var option = TransactionOption.newBuilder().setType(TransactionType.LONG).addWritePreserves(WritePreserve.newBuilder().setTableName("test")).build();
            var actual = REPORTER.toString(option);
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
            var actual = REPORTER.toString(option);
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
            var actual = REPORTER.toString(option);
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
            var actual = REPORTER.toString(option);
            var expected = "[" //
                    + "\n  type: RTX" //
                    + "\n]";
            assertEquals(expected, actual);
        }
    }
}
