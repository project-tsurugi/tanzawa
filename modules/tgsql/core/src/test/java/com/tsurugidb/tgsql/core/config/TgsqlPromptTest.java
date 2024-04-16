package com.tsurugidb.tgsql.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.tsurugidb.sql.proto.SqlRequest.ReadArea;
import com.tsurugidb.sql.proto.SqlRequest.TransactionOption;
import com.tsurugidb.sql.proto.SqlRequest.TransactionType;
import com.tsurugidb.sql.proto.SqlRequest.WritePreserve;
import com.tsurugidb.tgsql.core.executor.sql.TransactionWrapper;
import com.tsurugidb.tsubakuro.sql.Transaction;

class TgsqlPromptTest {

    @Test
    void empty() {
        var prompt = TgsqlPrompt.create("");
        assertNull(prompt);
    }

    @Test
    void constant() {
        var prompt = TgsqlPrompt.create("tgsql> ");
        String actual = prompt.getPrompt(null, null);
        assertEquals("tgsql> ", actual);
    }

    @Test
    void endpoint() {
        var prompt = TgsqlPrompt.create("{endpoint}> ");
        {
            var config = new TgsqlConfig();
            config.setEndpoint("tcp://localhost:12345");
            String actual = prompt.getPrompt(config, null);
            assertEquals("tcp://localhost:12345> ", actual);
        }
        {
            var config = new TgsqlConfig();
            String actual = prompt.getPrompt(config, null);
            assertEquals("null> ", actual);
        }
    }

    @Test
    void now() {
        {
            var prompt = TgsqlPrompt.create("{now}> ");
            String start = LocalDateTime.now().toString() + "> ";
            String actual = prompt.getPrompt(null, null);
            String end = LocalDateTime.now().toString() + "> ";
            if (start.compareTo(actual) <= 0 && actual.compareTo(end) <= 0) {
                // success
            } else {
                fail(String.format("now error. actual=[%s], start=[%s], end=[%s]", actual, start, end));
            }
        }
        {
            String format = "yyyy/MM/dd HHmmss.SSS";
            var formatter = DateTimeFormatter.ofPattern(format);

            var prompt = TgsqlPrompt.create("{now." + format + "}> ");
            String start = ZonedDateTime.now().format(formatter) + "> ";
            String actual = prompt.getPrompt(null, null);
            String end = ZonedDateTime.now().format(formatter) + "> ";
            if (start.compareTo(actual) <= 0 && actual.compareTo(end) <= 0) {
                // success
            } else {
                fail(String.format("now error. actual=[%s], start=[%s], end=[%s]", actual, start, end));
            }
        }
    }

    @Test
    void transactionId() {
        var prompt = TgsqlPrompt.create("{tx.id}> ");
        var tx = new Transaction() {
            @Override
            public String getTransactionId() {
                return "TID-12345";
            }
        };
        var transaction = new TransactionWrapper(tx, null);
        String actual = prompt.getPrompt(null, transaction);
        assertEquals("TID-12345> ", actual);
    }

    @Test
    void txType() {
        var prompt = TgsqlPrompt.create("type={tx.type}> ");
        var option = TransactionOption.newBuilder().setType(TransactionType.SHORT).build();
        var transaction = new TransactionWrapper(null, option);
        String actual = prompt.getPrompt(null, transaction);
        assertEquals("type=OCC> ", actual);
    }

    @Test
    void txLabel() {
        var prompt = TgsqlPrompt.create("label=[{tx.label}]> ");
        var option = TransactionOption.newBuilder().setLabel("abc").build();
        var transaction = new TransactionWrapper(null, option);
        String actual = prompt.getPrompt(null, transaction);
        assertEquals("label=[abc]> ", actual);
    }

    @ParameterizedTest
    @ValueSource(strings = { "tx.include-ddl", "tx.include_ddl", "tx.includeDdl" })
    void txIncludeDdl(String property) {
        var prompt = TgsqlPrompt.create("include_ddl={" + property + "}> ");
        var option = TransactionOption.newBuilder().setModifiesDefinitions(true).build();
        var transaction = new TransactionWrapper(null, option);
        String actual = prompt.getPrompt(null, transaction);
        assertEquals("include_ddl=true> ", actual);
    }

    @ParameterizedTest
    @ValueSource(strings = { "tx.wp", "tx.write-preserve" })
    void txWritePreserve(String property) {
        var prompt = TgsqlPrompt.create("{tx.type}(wp=[{" + property + "}])> ");
        var option = TransactionOption.newBuilder().setType(TransactionType.LONG) //
                .addWritePreserves(WritePreserve.newBuilder().setTableName("test1").build()) //
                .addWritePreserves(WritePreserve.newBuilder().setTableName("test2").build()) //
                .build();
        var transaction = new TransactionWrapper(null, option);
        String actual = prompt.getPrompt(null, transaction);
        assertEquals("LTX(wp=[\"test1\", \"test2\"])> ", actual);
    }

    @ParameterizedTest
    @ValueSource(strings = { "tx.ira", "tx.inclusive-read-area" })
    void txInclusiveReadArea(String property) {
        var prompt = TgsqlPrompt.create("{tx.type}(ra=[{" + property + "}])> ");
        var option = TransactionOption.newBuilder().setType(TransactionType.LONG) //
                .addInclusiveReadAreas(ReadArea.newBuilder().setTableName("test1").build()) //
                .addInclusiveReadAreas(ReadArea.newBuilder().setTableName("test2").build()) //
                .build();
        var transaction = new TransactionWrapper(null, option);
        String actual = prompt.getPrompt(null, transaction);
        assertEquals("LTX(ra=[\"test1\", \"test2\"])> ", actual);
    }

    @ParameterizedTest
    @ValueSource(strings = { "tx.era", "tx.exclusive-read-area" })
    void txExclusiveReadArea(String property) {
        var prompt = TgsqlPrompt.create("{tx.type}(ra=[{" + property + "}])> ");
        var option = TransactionOption.newBuilder().setType(TransactionType.LONG) //
                .addExclusiveReadAreas(ReadArea.newBuilder().setTableName("test1").build()) //
                .addExclusiveReadAreas(ReadArea.newBuilder().setTableName("test2").build()) //
                .build();
        var transaction = new TransactionWrapper(null, option);
        String actual = prompt.getPrompt(null, transaction);
        assertEquals("LTX(ra=[\"test1\", \"test2\"])> ", actual);
    }

    @Test
    void txPriority() {
        var prompt = TgsqlPrompt.create("{tx.priority}> ");
        var option = TransactionOption.newBuilder().build();
        var transaction = new TransactionWrapper(null, option);
        String actual = prompt.getPrompt(null, transaction);
        assertEquals("unspecified> ", actual);
    }

    @Test
    void brace1() {
        var prompt = TgsqlPrompt.create("{{abc}}");
        String actual = prompt.getPrompt(null, null);
        assertEquals("{abc}", actual);
    }

    @Test
    void brace2() {
        var prompt = TgsqlPrompt.create("tid={{{tx.id}}}> ");
        var tx = new com.tsurugidb.tsubakuro.sql.Transaction() {
            @Override
            public String getTransactionId() {
                return "TID-12345";
            }
        };
        var transaction = new TransactionWrapper(tx, null);
        String actual = prompt.getPrompt(null, transaction);
        assertEquals("tid={TID-12345}> ", actual);
    }
}
