package com.tsurugidb.console.cli;

import java.io.Closeable;
import java.io.StringReader;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.tsurugidb.console.cli.argument.CliArgument;
import com.tsurugidb.console.cli.config.ConsoleConfigBuilder;
import com.tsurugidb.console.cli.config.ExecConfigBuilder;
import com.tsurugidb.console.cli.config.ScriptConfigBuilder;
import com.tsurugidb.console.cli.explain.ExplainConvertRunner;
import com.tsurugidb.console.cli.repl.ReplCvKey;
import com.tsurugidb.console.cli.repl.ReplEngine;
import com.tsurugidb.console.cli.repl.ReplReporter;
import com.tsurugidb.console.cli.repl.ReplResultProcessor;
import com.tsurugidb.console.cli.repl.ReplScript;
import com.tsurugidb.console.cli.repl.ReplThreadExecutor;
import com.tsurugidb.console.cli.repl.jline.ReplJLineHistory;
import com.tsurugidb.console.cli.repl.jline.ReplJLineReader;
import com.tsurugidb.console.cli.repl.jline.ReplJLineTerminal;
import com.tsurugidb.console.core.ScriptRunner;

/**
 * A program entry of Tsurugi SQL console cli.
 */
public final class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

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

    /**
     * Execute script.
     *
     * @param args the program arguments
     * @return exit code
     * @throws Exception if exception was occurred
     */
    public static int execute(String... args) throws Exception {
        var argument = new CliArgument();
        var commander = JCommander.newBuilder() //
                .programName(Main.class.getName()) //
                .addObject(argument) //
                .build();
        try (Closeable c0 = () -> ReplJLineTerminal.close()) {
            commander.parse(args);

            if (argument.isHelp()) {
                commander.usage();
                return 0;
            }

            switch (argument.getCliMode()) {
            case CONSOLE:
                executeConsole(commander, argument);
                break;
            case EXEC:
                executeExec(commander, argument);
                break;
            case SCRIPT:
                executeScript(commander, argument);
                break;
            case EXPLAIN:
                ExplainConvertRunner.execute(argument);
                break;
            default:
                commander.usage();
                return 1;
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

    private static void executeConsole(JCommander commander, CliArgument argument) throws Exception {
        ReplCvKey.registerKey();

        var builder = new ConsoleConfigBuilder(argument);
        var config = builder.build();

        var lineReader = ReplJLineReader.createReader(config);
        config.setHistorySupplier(ReplJLineHistory.createHistorySupplier(lineReader.getHistory()));
        var script = new ReplScript(lineReader);
        var terminal = lineReader.getTerminal();
        var reporter = new ReplReporter(terminal, config);
        try (var executor = new ReplThreadExecutor("SQL engine", terminal); //
                var resultProcessor = new ReplResultProcessor(config, reporter)) {
            ScriptRunner.repl(script, config, engine -> new ReplEngine(engine, executor), resultProcessor, reporter);
        }
    }

    private static void executeExec(JCommander commander, CliArgument argument) throws Exception {
        var builder = new ExecConfigBuilder(argument);
        var config = builder.build();
        var statement = builder.getStatement();

        try (var reader = new StringReader(statement)) {
            ScriptRunner.execute(() -> reader, config);
        }
    }

    private static void executeScript(JCommander commander, CliArgument argument) throws Exception {
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
