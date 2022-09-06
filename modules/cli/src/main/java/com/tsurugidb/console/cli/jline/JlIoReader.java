package com.tsurugidb.console.cli.jline;

import java.io.IOException;
import java.io.Reader;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;

/**
 * Tsurugi SQL console java.io.Reader
 */
public class JlIoReader extends Reader {

    private static final String PROMPT1 = "tgsql> ";
    private static final String PROMPT2 = "     | ";

    private final LineReader lineReader;
    private String buffer = null;
    private int index;

    public JlIoReader(LineReader lineReader) throws IOException {
        this.lineReader = lineReader;

        lineReader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, PROMPT2);
    }

    // for test
    JlIoReader() {
        this.lineReader = null;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (this.buffer == null) {
            try {
                this.buffer = readBuffer();
            } catch (EndOfFileException e) {
                return -1;
            }
            this.index = 0;
        }

        int length = (off + len <= cbuf.length) ? len : cbuf.length - off;
        if (length <= 0) {
            return 0;
        }

        int remainSize = buffer.length() - index;
        if (remainSize <= length) {
            buffer.getChars(index, index + remainSize, cbuf, off);
            this.buffer = null;
            return remainSize;
        }

        buffer.getChars(index, index + length, cbuf, off);
        index += length;
        return length;
    }

    protected String readBuffer() {
        return lineReader.readLine(PROMPT1) + "\n";
    }

    @Override
    public void close() throws IOException {
        // do nothing
    }
}
