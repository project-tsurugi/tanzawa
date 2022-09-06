package com.tsurugidb.console.cli;

import com.tsurugidb.console.cli.jline.JLineMain;
import com.tsurugidb.console.core.ScriptRunner;

/**
 * A program entry of Tsurugi SQL console cli.
 */
public final class Main {

    /**
     * Executes a script file.
     * <ul>
     * <li>{@code args[0]} : path to the script file (UTF-8 encoded)</li>
     * <li>{@code args[1]} : connection URI</li>
     * </ul>
     * 
     * @param args the program arguments
     * @throws Exception if exception was occurred
     */
    public static void main(String... args) throws Exception {
        if (args.length >= 2 && args[0].equals("-")) {
            JLineMain.main(args);
            return;
        }

        // FIXME: use options parser
        ScriptRunner.main(args);
    }

    private Main() {
        throw new AssertionError();
    }
}
