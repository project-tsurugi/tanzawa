package com.tsurugidb.console.cli;

import java.io.Closeable;
import java.io.StringReader;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.tsurugidb.console.cli.argument.ConsoleArgument;
import com.tsurugidb.console.cli.argument.ExecArgument;
import com.tsurugidb.console.cli.argument.ScriptArgument;
import com.tsurugidb.console.cli.config.ConsoleConfigBuilder;
import com.tsurugidb.console.cli.config.ExecConfigBuilder;
import com.tsurugidb.console.cli.config.ScriptConfigBuilder;
import com.tsurugidb.console.cli.repl.ReplEngine;
import com.tsurugidb.console.cli.repl.ReplReporter;
import com.tsurugidb.console.cli.repl.ReplResultProcessor;
import com.tsurugidb.console.cli.repl.ReplScript;
import com.tsurugidb.console.cli.repl.ReplThreadExecutor;
import com.tsurugidb.console.cli.repl.jline.ReplJLineReader;
import com.tsurugidb.console.cli.repl.jline.ReplJLineTerminal;
import com.tsurugidb.console.core.ScriptRunner;

/**
 * A program entry of Tsurugi SQL console cli.
 */
public final class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    // command name
    private static final String CONSOLE = "console"; //$NON-NLS-1$
    private static final String EXEC = "exec"; //$NON-NLS-1$
    private static final String SCRIPT = "script"; //$NON-NLS-1$

    /**
     * Execute script.
     *
     * @param args the program arguments
     * @throws Exception if exception was occurred
     */
    public static void main(String... args) throws Exception {
        int exitCode = execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static int execute(String[] args) throws Exception {
        var consoleArgument = new ConsoleArgument();
        var execArgument = new ExecArgument();
        var scriptArgument = new ScriptArgument();
        var commander = JCommander.newBuilder() //
                .programName(Main.class.getName()) //
                .addCommand(CONSOLE, consoleArgument) //
                .addCommand(EXEC, execArgument) //
                .addCommand(SCRIPT, scriptArgument) //
                .build();
        try (Closeable c0 = () -> ReplJLineTerminal.close()) {
            commander.parse(args);

            String command = commander.getParsedCommand();
            if (command == null) {
                commander.usage();
                return 0;
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
            return 0;
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
                    return 1;
                }
            }

            commander.usage();
            return 1;
        }
    }

    private static void executeConsole(JCommander commander, ConsoleArgument argument) throws Exception {
        var builder = new ConsoleConfigBuilder(argument);
        var config = builder.build();

        var lineReader = ReplJLineReader.createReader();
        var script = new ReplScript(lineReader);
        var terminal = lineReader.getTerminal();
        var reporter = new ReplReporter(terminal);
        try (var executor = new ReplThreadExecutor("SQL engine", terminal); //
                var resultProcessor = new ReplResultProcessor(reporter)) {
            ScriptRunner.repl(script, config, engine -> new ReplEngine(engine, executor), resultProcessor, reporter);
        }
    }

    private static void executeExec(JCommander commander, ExecArgument argument) throws Exception {
        var builder = new ExecConfigBuilder(argument);
        var config = builder.build();
        var statement = builder.getStatement();

        try (var reader = new StringReader(statement)) {
            ScriptRunner.execute(() -> reader, config);
        }
    }

    private static void executeScript(JCommander commander, ScriptArgument argument) throws Exception {
        var builder = new ScriptConfigBuilder(argument);
        var config = builder.build();
        var script = builder.getScript();
        var encoding = builder.getEncoding();

        try (var reader = Files.newBufferedReader(script, encoding)) {
            ScriptRunner.execute(() -> reader, config);
        }
    }

    private Main() {
        throw new AssertionError();
    }
}
