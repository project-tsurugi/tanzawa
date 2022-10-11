package com.tsurugidb.console.cli.repl;

import javax.annotation.Nonnull;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.cli.repl.jline.ReplJLineParser.ParsedStatement;
import com.tsurugidb.console.core.exception.ScriptInterruptedException;
import com.tsurugidb.console.core.executor.IoSupplier;
import com.tsurugidb.console.core.model.Statement;

/**
 * Tsurugi SQL console repl script.
 */
public class ReplScript implements IoSupplier<Statement> {
    private static final Logger LOG = LoggerFactory.getLogger(ReplScript.class);

    private static final String PROMPT1 = "tgsql> "; //$NON-NLS-1$
    private static final String PROMPT2 = "     | "; //$NON-NLS-1$

    private final LineReader lineReader;

    /**
     * Creates a new instance.
     *
     * @param lineReader LineReader
     */
    public ReplScript(@Nonnull LineReader lineReader) {
        this.lineReader = lineReader;

        lineReader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, PROMPT2);
    }

    @Override
    public Statement get() {
        try {
            lineReader.readLine(PROMPT1);
        } catch (UserInterruptException e) {
            throw new ScriptInterruptedException(e);
        } catch (EndOfFileException e) {
            LOG.trace("EndOfFileException", e);
            return null;
        }

        var line = lineReader.getParsedLine();
        if (line instanceof ParsedStatement) {
            return ((ParsedStatement) line).statement();
        }
        throw new AssertionError(line);
    }
}
