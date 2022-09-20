package com.tsurugidb.console.cli.repl.jline;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import com.tsurugidb.console.cli.config.CliEnvironment;

/**
 * Tsurugi SQL console JLine LineReader.
 */
public final class ReplJLineReader {

    private static final String APP_NAME = "Tsurugi SQL console"; //$NON-NLS-1$

    /**
     * create LineReader.
     * 
     * @return LineReader
     */
    public static LineReader createReader() {
        var terminal = ReplJLineTerminal.getTerminal();

        var parser = new ReplJLineParser();
        parser.setEscapeChars(null);

        var reader = LineReaderBuilder.builder() //
                .appName(APP_NAME) //
                .terminal(terminal) //
                .parser(parser) //
                .build();
        CliEnvironment.findUserHomeReplHistoryPath().ifPresent(path -> {
            reader.setVariable(LineReader.HISTORY_FILE, path);
        });
        return reader;
    }

    /**
     * create simple LineReader.
     * 
     * @return LineReader
     */
    public static LineReader createSimpleReader() {
        var terminal = ReplJLineTerminal.getTerminal();

        var reader = LineReaderBuilder.builder() //
                .appName(APP_NAME) //
                .terminal(terminal) //
                .build();
        return reader;
    }

    private ReplJLineReader() {
        throw new AssertionError();
    }
}
