package com.tsurugidb.console.cli.repl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.jline.reader.EndOfFileException;
import org.jline.reader.UserInterruptException;
import org.junit.jupiter.api.Test;

import com.tsurugidb.console.core.exception.ScriptInterruptedException;

class ReplReaderTest {

    @Test
    void empty() throws IOException {
        try (var target = new ReplReaderTestMock("")) {
            var buf = new char[3];
            int len = target.read(buf, 0, buf.length);
            assertEquals(0, len);
            assertEqualsChars("\0\0\0", buf);
        }
    }

    @Test
    void eqBufSize() throws IOException {
        String text = "abc";
        try (var target = new ReplReaderTestMock(text)) {
            var buf = new char[text.length()];
            int len = target.read(buf, 0, buf.length);
            assertEquals(text.length(), len);
            assertEqualsChars(text, buf);
        }
    }

    @Test
    void startsBuf() throws IOException {
        String text = "abc";
        try (var target = new ReplReaderTestMock(text)) {
            var buf = new char[text.length() + 2];
            int len = target.read(buf, 0, text.length());
            assertEquals(text.length(), len);
            assertEqualsChars(text + "\0\0", buf);
        }
    }

    @Test
    void middleBuf() throws IOException {
        String text = "abc";
        try (var target = new ReplReaderTestMock(text)) {
            var buf = new char[text.length() + 2];
            int len = target.read(buf, 1, text.length());
            assertEquals(text.length(), len);
            assertEqualsChars("\0" + text + "\0", buf);
        }
    }

    @Test
    void endsBuf() throws IOException {
        String text = "abc";
        try (var target = new ReplReaderTestMock(text)) {
            var buf = new char[text.length() + 2];
            int len = target.read(buf, 2, text.length());
            assertEquals(text.length(), len);
            assertEqualsChars("\0\0" + text, buf);
        }
    }

    @Test
    void endsBufOver() throws IOException {
        String text = "abc";
        try (var target = new ReplReaderTestMock(text)) {
            var buf = new char[text.length() + 2];
            int len = target.read(buf, 3, text.length());
            assertEquals(text.length() - 1, len);
            assertEqualsChars("\0\0\0" + text.substring(0, text.length() - 1), buf);
        }
    }

    @Test
    void split() throws IOException {
        try (var target = new ReplReaderTestMock("abcdef")) {
            var buf1 = new char[3];
            int len1 = target.read(buf1);
            assertEquals(buf1.length, len1);
            assertEqualsChars("abc", buf1);

            var buf2 = new char[2];
            int len2 = target.read(buf2);
            assertEquals(buf2.length, len2);
            assertEqualsChars("de", buf2);

            var buf3 = new char[2];
            int len3 = target.read(buf3);
            assertEquals(1, len3);
            assertEqualsChars("f\0", buf3);
        }
    }

    @Test
    void multiLine() throws IOException {
        try (var target = new ReplReaderTestMock("abc", "def")) {
            var buf1 = new char[3];
            int len1 = target.read(buf1);
            assertEquals(buf1.length, len1);
            assertEqualsChars("abc", buf1);

            var buf2 = new char[2];
            int len2 = target.read(buf2);
            assertEquals(buf2.length, len2);
            assertEqualsChars("de", buf2);

            var buf3 = new char[2];
            int len3 = target.read(buf3);
            assertEquals(1, len3);
            assertEqualsChars("f\0", buf3);
        }
    }

    @Test
    void multiLine2() throws IOException {
        try (var target = new ReplReaderTestMock("abc", "def")) {
            var buf1 = new char[2];
            int len1 = target.read(buf1);
            assertEquals(buf1.length, len1);
            assertEqualsChars("ab", buf1);

            var buf2 = new char[2];
            int len2 = target.read(buf2);
            assertEquals(1, len2);
            assertEqualsChars("c\0", buf2);

            var buf3 = new char[2];
            int len3 = target.read(buf3);
            assertEquals(buf3.length, len3);
            assertEqualsChars("de", buf3);

            var buf4 = new char[2];
            int len4 = target.read(buf4);
            assertEquals(1, len4);
            assertEqualsChars("f\0", buf4);
        }
    }

    @Test
    void interrupt() throws IOException {
        try (var target = new ReplReaderTestMock() {
            @Override
            protected String readBuffer() {
                throw new UserInterruptException(null);
            }
        }) {
            var buf = new char[16];
            assertThrows(ScriptInterruptedException.class, () -> target.read(buf, 0, buf.length));
        }
    }

    @Test
    void eof() throws IOException {
        try (var target = new ReplReaderTestMock() {
            private int count = 0;

            @Override
            protected String readBuffer() {
                if (count++ == 0) {
                    return super.readBuffer();
                }
                throw new AssertionError();
            }
        }) {
            var buf = new char[16];
            int len = target.read(buf, 0, buf.length);
            assertEquals(-1, len);

            int len2 = target.read(buf, 0, buf.length);
            assertEquals(-1, len2);
        }
    }

    private static class ReplReaderTestMock extends ReplReader {

        private String[] text;
        private int index = 0;

        public ReplReaderTestMock(String... text) {
            this.text = text;
        }

        @Override
        protected String readBuffer() {
            if (index >= text.length) {
                throw new EndOfFileException();
            }
            return text[index++];
        }
    }

    private static void assertEqualsChars(String expected, char[] actual) {
        var expectedBuf = new char[expected.length()];
        expected.getChars(0, expected.length(), expectedBuf, 0);
        assertArrayEquals(expectedBuf, actual);
    }
}
