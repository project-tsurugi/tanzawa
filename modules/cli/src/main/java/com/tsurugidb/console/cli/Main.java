package com.tsurugidb.console.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.tsurugidb.console.cli.argument.ConfigUtil;
import com.tsurugidb.console.cli.argument.ConsoleArgument;
import com.tsurugidb.console.cli.argument.ExecArgument;
import com.tsurugidb.console.cli.argument.ScriptArgument;
import com.tsurugidb.console.cli.repl.ReplLineReader;
import com.tsurugidb.console.cli.repl.ReplReader;
import com.tsurugidb.console.cli.repl.ReplReporter;
import com.tsurugidb.console.cli.repl.ReplResultProcessor;
import com.tsurugidb.console.core.ScriptRunner;
import com.tsurugidb.console.core.config.ScriptConfig;

/**
 * A program entry of Tsurugi SQL console cli.
 */
public final class Main {

    // command name
    private static final String CONSOLE = "console";
    private static final String EXEC = "exec";
    private static final String SCRIPT = "script";

    /**
     * Execute script.
     * 
     * @param args the program arguments
     * @throws Exception if exception was occurred
     */
    public static void main(String... args) throws Exception {
        var consoleArgument = new ConsoleArgument();
        var execArgument = new ExecArgument();
        var scriptArgument = new ScriptArgument();
        var commander = JCommander.newBuilder() //
                .programName("tsurugi-sql") //
                .addCommand(CONSOLE, consoleArgument) //
                .addCommand(EXEC, execArgument) //
                .addCommand(SCRIPT, scriptArgument) //
                .build();
        try {
            commander.parse(args);

            String command = commander.getParsedCommand();
            if (command == null) {
                commander.usage();
                return;
            }
            switch (command) {
            case CONSOLE:
                executeConsole(commander, consoleArgument);
                break;
            case EXEC:
                executeExec(commander, execArgument);
                break;
            case SCRIPT:
                executeScript(commander, scriptArgument);
                break;
            default:
                throw new AssertionError(command);
            }
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            var c = e.getJCommander();
            if (c != null) {
                c.usage();
            } else {
                commander.usage();
            }
            System.exit(1);
        }
    }

    private static void executeConsole(JCommander commander, ConsoleArgument argument) throws Exception {
        var config = new ScriptConfig();
        try {
            ConfigUtil.fillConsoleConfig(config, argument);
        } catch (ParameterException e) {
            var c = commander.getCommands().get(CONSOLE);
            e.setJCommander(c);
            throw e;
        }

        var lineReader = ReplLineReader.create();
        var reporter = new ReplReporter(lineReader.getTerminal());
        try (var reader = new ReplReader(lineReader); //
                var resultProcessor = new ReplResultProcessor(reporter)) {
            ScriptRunner.repl(config, reader, resultProcessor, reporter);
        }
    }

    private static void executeExec(JCommander commander, ExecArgument argument) throws Exception {
        var config = new ScriptConfig();
        try {
            ConfigUtil.fillExecConfig(config, argument);
        } catch (ParameterException e) {
            var c = commander.getCommands().get(EXEC);
            e.setJCommander(c);
            throw e;
        }

        // TODO Auto-generated method stub

    }

    private static void executeScript(JCommander commander, ScriptArgument argument) throws Exception {
        var config = new ScriptConfig();
        try {
            ConfigUtil.fillScriptConfig(config, argument);
        } catch (ParameterException e) {
            var c = commander.getCommands().get(SCRIPT);
            e.setJCommander(c);
            throw e;
        }

        ScriptRunner.execute(argument.getScript(), config);
    }

    private Main() {
        throw new AssertionError();
    }
}
