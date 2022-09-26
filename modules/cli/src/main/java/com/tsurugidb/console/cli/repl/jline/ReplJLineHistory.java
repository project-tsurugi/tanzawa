package com.tsurugidb.console.cli.repl.jline;

import java.io.IOException;

import org.jline.reader.impl.history.DefaultHistory;

/**
 * Tsurugi SQL console JLine History.
 */
public class ReplJLineHistory extends DefaultHistory {

    @Override
    public void load() throws IOException {
        read(null, true);
    }
}
