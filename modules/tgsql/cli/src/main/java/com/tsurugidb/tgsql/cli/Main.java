package com.tsurugidb.tgsql.cli;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.tsurugidb.tgsql.cli.argument.CliArgument;
import com.tsurugidb.tgsql.cli.config.ConsoleConfigBuilder;
import com.tsurugidb.tgsql.cli.config.ExecConfigBuilder;
import com.tsurugidb.tgsql.cli.config.ScriptConfigBuilder;
import com.tsurugidb.tgsql.cli.explain.ExplainConvertRunner;
import com.tsurugidb.tgsql.cli.repl.ReplCvKey;
import com.tsurugidb.tgsql.cli.repl.ReplEngine;
import com.tsurugidb.tgsql.cli.repl.ReplReporter;
import com.tsurugidb.tgsql.cli.repl.ReplResultProcessor;
import com.tsurugidb.tgsql.cli.repl.ReplScript;
import com.tsurugidb.tgsql.cli.repl.ReplThreadExecutor;
import com.tsurugidb.tgsql.cli.repl.jline.ReplJLineHistory;
import com.tsurugidb.tgsql.cli.repl.jline.ReplJLineReader;
import com.tsurugidb.tgsql.cli.repl.jline.ReplJLineTerminal;
import com.tsurugidb.tgsql.core.ScriptRunner;
import com.tsurugidb.tools.common.util.LibraryVersion;
import com.tsurugidb.tsubakuro.client.ServiceClientCollector;
import com.tsurugidb.tsubakuro.util.TsubakuroVersion;

/**
 * A program entry of Tsurugi SQL console cli.
 */
public final class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static final String TGSQL_CORE_MODULE_NAME = "tanzawa-tgsql-core"; //$NON-NLS-1$

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
                .programName("tgsql") //$NON-NLS-1$
                .addObject(argument) //
                .build();
        try (Closeable c0 = () -> ReplJLineTerminal.close()) {
            commander.parse(args);

            if (argument.isVersion()) {
                printVersion();
                // return
            }
            if (argument.isHelp()) {
                commander.usage();
                return 0;
            }
            if (argument.isVersion()) {
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

            commander.usage();
            return 1;
        }
    }

    private static void printVersion() {
        var tgsqlCoreVersion = getTgSqlCoreVersion();

        String tgsqlVersion = getVersion("tgsqlVersion", () -> tgsqlCoreVersion.getBuildVersion().get());
        System.out.println("-------------------------------------");
        System.out.printf("Tsurugi SQL console %s%n", tgsqlVersion);
        System.out.println("-------------------------------------");

        System.out.println();
        String tgsqlTimestamp = getVersion("tgsqlTimestamp", () -> tgsqlCoreVersion.getBuildTimestamp().get().toString());
        System.out.printf("Build time: %s%n", tgsqlTimestamp);

        System.out.println();
        String tsubakuroVersion = getVersion("tsubakuroVersion", () -> TsubakuroVersion.getBuildVersion("session"));
        System.out.printf("Tsubakuro:  %s%n", tsubakuroVersion);
        String serviceMessageVersion = getServiceMessageVersion();
        System.out.printf("  service message version: %s%n", serviceMessageVersion);
        String jvmVersion = getVersion("jvmVersion", () -> System.getProperty("java.vm.version"));
        String javaHome = getVersion("javaHome", () -> System.getProperty("java.home"));
        System.out.printf("JVM:        %s (%s)%n", jvmVersion, javaHome);
    }

    private static LibraryVersion getTgSqlCoreVersion() {
        try {
            return LibraryVersion.loadByName(TGSQL_CORE_MODULE_NAME, Main.class.getClassLoader());
        } catch (Exception e) {
            LOG.debug("getTgSqlCoreVersion error", e);
            return null;
        }
    }

    @FunctionalInterface
    private interface StringSupplier {
        String get() throws IOException;
    }

    private static String getVersion(String title, StringSupplier supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            LOG.debug(title + " get error", e);
            return "unknown";
        }
    }

    private static String getServiceMessageVersion() {
        try {
            var classList = ServiceClientCollector.collect(false);
            return classList.stream().flatMap(c -> ServiceClientCollector.findServiceMessageVersion(c).stream()).collect(Collectors.joining(", "));
        } catch (Exception e) {
            LOG.debug("getServiceMessageVersion error", e);
            return "N/A";
        }
    }

    private static void executeConsole(JCommander commander, CliArgument argument) throws Exception {
        argument.checkUnknownParameter();

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
        LOG.debug("exec.statement=[{}]", statement);

        try (var reader = new StringReader(statement)) {
            ScriptRunner.execute(() -> reader, config);
        }
    }

    private static void executeScript(JCommander commander, CliArgument argument) throws Exception {
        var builder = new ScriptConfigBuilder(argument);
        var config = builder.build();
        var script = builder.getScript();
        LOG.debug("script.script=[{}]", script);
        var encoding = builder.getEncoding();
        LOG.debug("script.encoding=[{}]", encoding);

        try (var reader = Files.newBufferedReader(script, encoding)) {
            ScriptRunner.execute(() -> reader, config);
        }
    }

    private Main() {
        throw new AssertionError();
    }
}
