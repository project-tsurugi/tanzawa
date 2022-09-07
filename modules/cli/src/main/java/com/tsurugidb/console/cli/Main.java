package com.tsurugidb.console.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.tsurugidb.console.cli.jline.JLineMain;
import com.tsurugidb.console.core.ScriptRunner;

/**
 * A program entry of Tsurugi SQL console cli.
 */
public final class Main {

    /**
     * Executes a script file.
     * <ul>
     * <li>{@code --file} : path to the script file (UTF-8 encoded)</li>
     * <li>{@code --endpoint} : connection URI</li>
     * </ul>
     * 
     * @param args the program arguments
     * @throws Exception if exception was occurred
     */
    public static void main(String... args) throws Exception {
        var argument = new CliArgument();
        var commander = JCommander.newBuilder() //
                .programName("tgsql") //
                .addObject(argument) //
                .build();
        try {
            commander.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            e.usage();
            System.exit(1);
        }

        if (argument.isHelp()) {
            commander.usage();
            return;
        }

        if (argument.isStdin()) {
            JLineMain.main(argument);
            return;
        }

        // FIXME: use options class
        ScriptRunner.main(argument.getScriptFile(), argument.getEndpoint());
    }

    private Main() {
        throw new AssertionError();
    }
}
