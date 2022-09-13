package com.tsurugidb.console.cli;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * A program entry of Tsurugi SQL console cli.
 */
public final class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

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
                .programName(Main.class.getName()) //
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
            if (LOG.isDebugEnabled()) {
                LOG.error(e.getMessage(), e);
            } else {
                LOG.error(e.getMessage());
            }

            String command = commander.getParsedCommand();
            if (command != null) {
                var c = commander.getCommands().get(command);
                if (c != null) {
                    c.usage();
                    System.exit(1);
                }
            }

            commander.usage();
            System.exit(1);
        }
    }

    private static void executeConsole(JCommander commander, ConsoleArgument argument) throws Exception {
        var config = ConfigUtil.createConsoleConfig(argument);

        var lineReader = ReplLineReader.create();
        var reporter = new ReplReporter(lineReader.getTerminal());
        try (var reader = new ReplReader(lineReader); //
                var resultProcessor = new ReplResultProcessor(reporter)) {
            ScriptRunner.repl(config, reader, resultProcessor, reporter);
        }
    }

    private static void executeExec(JCommander commander, ExecArgument argument) throws Exception {
        var config = ConfigUtil.createExecConfig(argument);

        var statement = argument.getStatement();
        LOG.debug("statement=[{}]", statement);

        try (var reader = new StringReader(statement)) {
            ScriptRunner.execute(() -> reader, config);
        }
    }

    private static void executeScript(JCommander commander, ScriptArgument argument) throws Exception {
        var config = ConfigUtil.createScriptConfig(argument);

        var script = Path.of(argument.getScript());
        LOG.debug("script={}", script);
        Charset encoding;
        try {
            encoding = Charset.forName(argument.getEncoding());
        } catch (Exception e) {
            throw new RuntimeException("invalid encoding", e);
        }
        LOG.debug("encoding={}", encoding);

        try (var reader = Files.newBufferedReader(script, encoding)) {
            ScriptRunner.execute(() -> reader, config);
        }
    }

    private Main() {
        throw new AssertionError();
    }
}
