package com.tsurugidb.console.cli.repl;

import java.io.IOException;
import java.io.Reader;

import javax.annotation.Nonnull;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;

import com.tsurugidb.console.core.exception.ScriptInterruptedException;

/**
 * Tsurugi SQL console repl Reader
 */
public class ReplReader extends Reader {

    private static final String PROMPT1 = "tgsql> ";
    private static final String PROMPT2 = "     | ";

    private final LineReader lineReader;
    private String buffer = null;
    private int index;
    private boolean eof = false;

    public ReplReader(@Nonnull LineReader lineReader) throws IOException {
        this.lineReader = lineReader;

        lineReader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, PROMPT2);
    }

    /**
     * for test
     */
    ReplReader() {
        this.lineReader = null;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if (this.eof) {
            return -1;
        }

        if (this.buffer == null) {
            try {
                this.buffer = readBuffer();
            } catch (UserInterruptException e) {
                throw new ScriptInterruptedException(e);
            } catch (EndOfFileException e) {
                this.eof = true;
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
