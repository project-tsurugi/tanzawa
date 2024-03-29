package com.tsurugidb.console.cli.repl;

import java.util.List;

import javax.annotation.Nonnull;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.cli.repl.jline.ReplJLineParser.ParsedStatement;
import com.tsurugidb.console.core.exception.ScriptInterruptedException;
import com.tsurugidb.console.core.executor.IoSupplier;
import com.tsurugidb.console.core.model.Region;
import com.tsurugidb.console.core.model.SimpleStatement;
import com.tsurugidb.console.core.model.Statement;
import com.tsurugidb.console.core.model.Statement.Kind;

/**
 * Tsurugi SQL console repl script.
 */
public class ReplScript implements IoSupplier<List<Statement>> {
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
    public List<Statement> get() {
        String text;
        try {
            text = lineReader.readLine(PROMPT1);
        } catch (UserInterruptException e) {
            throw new ScriptInterruptedException(e);
        } catch (EndOfFileException e) {
            LOG.trace("EndOfFileException", e); //$NON-NLS-1$
            return null;
        }

        var line = lineReader.getParsedLine();
        if (line instanceof ParsedStatement) {
            var statementList = ((ParsedStatement) line).statements();
            if (!statementList.isEmpty()) {
                return statementList;
            }
            var s = new SimpleStatement(Kind.EMPTY, text, new Region(0, text.length(), 0, 0));
            return List.of(s);
        }
        throw new AssertionError(line);
    }
}
