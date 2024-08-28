/*
 * Copyright 2023-2024 Project Tsurugi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tsurugidb.tgsql.cli.repl.jline;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import com.tsurugidb.tgsql.cli.config.CliEnvironment;
import com.tsurugidb.tgsql.core.config.TgsqlConfig;

/**
 * Tsurugi SQL console JLine LineReader.
 */
public final class ReplJLineReader {

    private static final String APP_NAME = "Tsurugi SQL console"; //$NON-NLS-1$

    /**
     * Creates a new instance.
     *
     * @param config tgsql configuration
     * @return LineReader
     */
    public static LineReader createReader(TgsqlConfig config) {
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
