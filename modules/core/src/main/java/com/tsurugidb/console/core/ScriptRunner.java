package com.tsurugidb.console.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.config.ScriptCvKey;
import com.tsurugidb.console.core.exception.ScriptInterruptedException;
import com.tsurugidb.console.core.exception.ScriptMessageException;
import com.tsurugidb.console.core.exception.ScriptNoMessageException;
import com.tsurugidb.console.core.executor.IoSupplier;
import com.tsurugidb.console.core.executor.engine.AbstractEngine;
import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.Engine;
import com.tsurugidb.console.core.executor.report.BasicReporter;
import com.tsurugidb.console.core.executor.report.ScriptReporter;
import com.tsurugidb.console.core.executor.result.BasicResultProcessor;
import com.tsurugidb.console.core.executor.result.ResultProcessor;
import com.tsurugidb.console.core.executor.sql.BasicSqlProcessor;
import com.tsurugidb.console.core.model.Statement;
import com.tsurugidb.console.core.model.Statement.Kind;
import com.tsurugidb.console.core.parser.SqlParser;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Executes SQL scripts.
 */
public final class ScriptRunner {

    static final Logger LOG = LoggerFactory.getLogger(ScriptRunner.class);

    private static final String NAME_STANDARD_INPUT = "-"; //$NON-NLS-1$

    private static final Charset DEFAULT_SCRIPT_ENCODING = StandardCharsets.UTF_8;

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
        if (args.length != 2) {
            throw new IllegalArgumentException(MessageFormat.format(//
                    "usage: java {0} </path/to/script.sql> <connection-uri>", //
                    ScriptRunner.class.getName()));
        }
        LOG.debug("script: {}", args[0]); //$NON-NLS-1$
        LOG.debug("endpoint: {}", args[1]); //$NON-NLS-1$
        var script = args[0];
        var endpoint = args[1];
        Credential credential = NullCredential.INSTANCE;
        boolean success = execute(toReaderSupplier(script), endpoint, credential);
        if (!success) {
            System.exit(1);
        }
    }

    /**
     * Executes the script using basic implementation.
     *
     * @param script     the script file
     * @param endpoint   the connection target end-point URI
     * @param credential the connection credential information
     * @return {@code true} if successfully completed, {@code false} otherwise
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while establishing connection
     * @throws InterruptedException if interrupted while establishing connection
     */
    public static boolean execute(//
            @Nonnull IoSupplier<? extends Reader> script, //
            @Nonnull String endpoint, //
            @Nonnull Credential credential) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(script);
        Objects.requireNonNull(endpoint);
        Objects.requireNonNull(credential);

        var config = new ScriptConfig();
        config.setEndpoint(endpoint);
        config.setCredential(credential);

        return execute(script, config);
    }

    /**
     * Executes the script using basic implementation.
     *
     * @param script the script file
     * @param config script configuration
     * @return {@code true} if successfully completed, {@code false} otherwise
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while establishing connection
     * @throws InterruptedException if interrupted while establishing connection
     */
    public static boolean execute(//
            @Nonnull String script, //
            @Nonnull ScriptConfig config) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(script);
        Objects.requireNonNull(config);
        return execute(toReaderSupplier(script), config);
    }

    /**
     * Executes the script using basic implementation.
     *
     * @param script the script file
     * @param config script configuration
     * @return {@code true} if successfully completed, {@code false} otherwise
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while establishing connection
     * @throws InterruptedException if interrupted while establishing connection
     */
    public static boolean execute(//
            @Nonnull IoSupplier<? extends Reader> script, //
            @Nonnull ScriptConfig config) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(script);
        Objects.requireNonNull(config);

        try (var sqlProcessor = new BasicSqlProcessor(); //
                var resultProcessor = new BasicResultProcessor()) {
            var reporter = new BasicReporter();
            return execute(script, new BasicEngine(config, sqlProcessor, resultProcessor, reporter));
        }
    }

    /**
     * Executes the script.
     *
     * @param script the script file
     * @param engine the statement executor
     * @return {@code true} if successfully completed, {@code false} otherwise
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while establishing connection
     * @throws InterruptedException if interrupted while establishing connection
     */
    public static boolean execute(//
            @Nonnull IoSupplier<? extends Reader> script, //
            @Nonnull Engine engine) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(script);
        Objects.requireNonNull(engine);

        prepareConnect(engine);

        LOG.info("start processing script");
        try (var parser = new SqlParser(script.get())) {
            while (true) {
                Statement statement = parser.next();
                if (statement == null) {
                    LOG.trace("EOF");
                    break;
                }
                try {
                    boolean cont = engine.execute(statement);
                    if (!cont) {
                        LOG.info("shutdown was requested");
                        break;
                    }
                } catch (Exception e) {
                    LOG.error("exception was occurred while processing statement: text=''{}'', line={}, column={}", //
                            statement.getText(), //
                            statement.getRegion().getStartLine() + 1, //
                            statement.getRegion().getStartColumn() + 1, //
                            e);
                    engine.finish(false);
                    return false;
                }
            }
        }
        engine.finish(true);
        LOG.info("script execution was successfully completed");
        return true;
    }

    private static IoSupplier<? extends Reader> toReaderSupplier(String script) throws FileNotFoundException {
        if (script.equals(NAME_STANDARD_INPUT)) {
            LOG.debug("read SQL script from standard input"); //$NON-NLS-1$
            return () -> {
                var console = System.console();
                if (console != null) {
                    return console.reader();
                }
                return new InputStreamReader(System.in, Charset.defaultCharset());
            };
        }
        var path = Path.of(script);
        if (!Files.isRegularFile(path)) {
            throw new FileNotFoundException(path.toString());
        }
        LOG.debug("read SQL script from file: {}", path); //$NON-NLS-1$
        return () -> Files.newBufferedReader(path, DEFAULT_SCRIPT_ENCODING);
    }

    /**
     * Executes REPL.
     *
     * @param script          console reader
     * @param config          script configuration
     * @param resultProcessor result processer
     * @param reporter        reporter
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while establishing connection
     * @throws InterruptedException if interrupted while establishing connection
     */
    public static void repl(//
            @Nonnull IoSupplier<? extends List<Statement>> script, //
            @Nonnull ScriptConfig config, //
            @Nonnull Function<AbstractEngine, Engine> engineWrapper, //
            @Nonnull ResultProcessor resultProcessor, //
            @Nonnull ScriptReporter reporter) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(config);
        Objects.requireNonNull(script);
        Objects.requireNonNull(resultProcessor);

        try (var sqlProcessor = new BasicSqlProcessor()) {
            var engine = new BasicEngine(config, sqlProcessor, resultProcessor, reporter);
            repl(script, engineWrapper.apply(engine));
        }
    }

    /**
     * Executes REPL.
     *
     * @param script console reader
     * @param engine the statement executor
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while establishing connection
     * @throws InterruptedException if interrupted while establishing connection
     */
    public static void repl(//
            @Nonnull IoSupplier<? extends List<Statement>> script, //
            @Nonnull Engine engine) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(script);
        Objects.requireNonNull(engine);

        prepareConnect(engine);

        LOG.info("start repl");
        loop: while (true) {
            try {
                List<Statement> statementList = script.get();
                if (statementList == null) {
                    LOG.trace("EOF");
                    break;
                }
                for (var statement : statementList) {
                    if (statementList.size() >= 2 && statement.getKind() != Kind.EMPTY) {
                        var reporter = engine.getReporter();
                        reporter.implicit(statement.getText());
                    }

                    boolean cont = engine.execute(statement);
                    if (!cont) {
                        LOG.trace("shutdown was requested");
                        break loop;
                    }
                }
            } catch (ScriptInterruptedException e) {
                LOG.trace("user interrupted", e);
            } catch (ScriptMessageException e) {
                LOG.trace("message exception", e);
                var reporter = engine.getReporter();
                reporter.warn(e.getMessage());
                long time = e.getTimingTime();
                if (time != 0) {
                    var clientVariableMap = engine.getConfig().getClientVariableMap();
                    boolean timing = clientVariableMap.get(ScriptCvKey.TIMING, false);
                    if (timing) {
                        reporter.reportTiming(time);
                    }
                }
            } catch (ScriptNoMessageException e) {
                LOG.trace("no message exception", e);
            } catch (ServerException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.warn("{}", e.getDiagnosticCode().name(), e);
                }
                engine.getReporter().warn(e);
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null) {
                    message = e.getClass().getName();
                }
                if (LOG.isDebugEnabled()) {
                    LOG.warn("{}", message, e);
                }
                engine.getReporter().warn(message);
            }
        }
        engine.finish(true);
        LOG.info("repl execution was successfully completed");
    }

    private static void prepareConnect(Engine engine) throws ServerException, IOException, InterruptedException {
        String endpoint = engine.getConfig().getEndpoint();
        if (endpoint != null) {
            LOG.info("establishing connection: {}", endpoint);
            engine.connect();
        }
    }

    private ScriptRunner() {
        throw new AssertionError();
    }
}
