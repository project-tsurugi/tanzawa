package com.tsurugidb.tgsql.cli.repl.jline;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import com.tsurugidb.tgsql.cli.config.CliEnvironment;
import com.tsurugidb.tgsql.core.config.ScriptConfig;

/**
 * Tsurugi SQL console JLine LineReader.
 */
public final class ReplJLineReader {

    private static final String APP_NAME = "Tsurugi SQL console"; //$NON-NLS-1$

    /**
     * Creates a new instance.
     *
     * @param config script configuration
     * @return LineReader
     */
    public static LineReader createReader(ScriptConfig config) {
        var terminal = ReplJLineTerminal.getTerminal();
        var completer = new ReplJLineCompleter(config);
        var parser = new ReplJLineParser();
        var history = new ReplJLineHistory();

        var reader = LineReaderBuilder.builder() //
                .appName(APP_NAME) //
                .terminal(terminal) //
                .completer(completer) //
                .parser(parser) //
                .history(history) //
                .build();
        CliEnvironment.findUserHomeConsoleHistoryPath().ifPresent(path -> {
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
