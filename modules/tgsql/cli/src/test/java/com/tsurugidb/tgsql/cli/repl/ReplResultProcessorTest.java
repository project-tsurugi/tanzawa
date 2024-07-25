package com.tsurugidb.tgsql.cli.repl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class ReplResultProcessorTest {

    @Test
    void appendTo() {
        test("abc", "abc");
        test("123", 123);
        test("123", BigDecimal.valueOf(123));

        test("2024-07-25", LocalDate.of(2024, 7, 25));
        test("2024-07-25 01:02:03.123456789", LocalDateTime.of(2024, 7, 25, 1, 2, 3, 123456789));
        test("2024-07-25 23:59:59.000000000", LocalDateTime.of(2024, 7, 25, 23, 59, 59, 0));
        test("2024-07-25 01:02:03.123456789+09:00", OffsetDateTime.of(2024, 7, 25, 1, 2, 3, 123456789, ZoneOffset.ofHours(9)));
        test("2024-07-25 23:59:59.000000000+09:00", OffsetDateTime.of(2024, 7, 25, 23, 59, 59, 0, ZoneOffset.ofHours(9)));
        test("2024-07-25 23:59:59.000000000Z", OffsetDateTime.of(2024, 7, 25, 23, 59, 59, 0, ZoneOffset.UTC));
        test("01:02:03.123456789", LocalTime.of(1, 2, 3, 123456789));
        test("23:59:59.000000000", LocalTime.of(23, 59, 59, 0));
        test("01:02:03.123456789+09:00", OffsetTime.of(1, 2, 3, 123456789, ZoneOffset.ofHours(9)));
        test("23:59:59.000000000+09:00", OffsetTime.of(23, 59, 59, 0, ZoneOffset.ofHours(9)));
        test("23:59:59.000000000Z", OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC));

        test("0123456789abcdef", new byte[] { 0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef });
    }

    private void test(String expected, Object value) {
        var sb = new StringBuilder();

        try (var target = new ReplResultProcessor()) {
            target.appendTo(sb, value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        assertEquals(expected, sb.toString());
    }
}
