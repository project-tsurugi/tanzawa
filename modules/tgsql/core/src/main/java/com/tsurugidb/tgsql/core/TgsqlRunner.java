/*
 * Copyright 2023-2025 Project Tsurugi.
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
package com.tsurugidb.tgsql.core;

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
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.config.TgsqlCvKey;
import com.tsurugidb.tgsql.core.exception.TgsqlInterruptedException;
import com.tsurugidb.tgsql.core.exception.TgsqlMessageException;
import com.tsurugidb.tgsql.core.exception.TgsqlNoMessageException;
import com.tsurugidb.tgsql.core.executor.IoSupplier;
import com.tsurugidb.tgsql.core.executor.engine.AbstractEngine;
import com.tsurugidb.tgsql.core.executor.engine.BasicEngine;
import com.tsurugidb.tgsql.core.executor.engine.Engine;
import com.tsurugidb.tgsql.core.executor.report.BasicReporter;
import com.tsurugidb.tgsql.core.executor.report.TgsqlReporter;
import com.tsurugidb.tgsql.core.executor.result.BasicResultProcessor;
import com.tsurugidb.tgsql.core.executor.result.ResultProcessor;
import com.tsurugidb.tgsql.core.executor.sql.BasicSqlProcessor;
import com.tsurugidb.tgsql.core.executor.sql.TransactionWrapper;
import com.tsurugidb.tgsql.core.model.Statement;
import com.tsurugidb.tgsql.core.model.Statement.Kind;
import com.tsurugidb.tgsql.core.parser.SqlParser;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Executes SQL scripts.
 */
public final class TgsqlRunner {

    static final Logger LOG = LoggerFactory.getLogger(TgsqlRunner.class);

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
                    TgsqlRunner.class.getName()));
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

        var config = new TgsqlConfig();
        config.setEndpoint(endpoint);
        config.setCredential(() -> credential);

        return execute(script, config);
    }

    /**
     * Executes the script using basic implementation.
     *
     * @param script the script file
     * @param config tgsql configuration
     * @return {@code true} if successfully completed, {@code false} otherwise
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while establishing connection
     * @throws InterruptedException if interrupted while establishing connection
     */
    public static boolean execute(//
            @Nonnull String script, //
            @Nonnull TgsqlConfig config) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(script);
        Objects.requireNonNull(config);
        return execute(toReaderSupplier(script), config);
    }

    /**
     * Executes the script using basic implementation.
     *
     * @param script the script file
     * @param config tgsql configuration
     * @return {@code true} if successfully completed, {@code false} otherwise
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while establishing connection
     * @throws InterruptedException if interrupted while establishing connection
     */
    public static boolean execute(//
            @Nonnull IoSupplier<? extends Reader> script, //
            @Nonnull TgsqlConfig config) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(script);
        Objects.requireNonNull(config);

        try (var sqlProcessor = new BasicSqlProcessor(); //
                var resultProcessor = new BasicResultProcessor()) {
            var reporter = new BasicReporter(config);
            return execute(script, new BasicEngine(config, sqlProcessor, resultProcessor, reporter));
        }
    }

    /**
     * Executes the script.
     *
     * @param script the script file
     * @param engine the statement executor
     * @return {@code true} if successfully completed, {@code false} otherwise
     * @throws IOException          if I/O error was occurred while establishing connection
     * @throws InterruptedException if interrupted while establishing connection
     */
    public static boolean execute(//
            @Nonnull IoSupplier<? extends Reader> script, //
            @Nonnull Engine engine) throws IOException, InterruptedException {
        Objects.requireNonNull(script);
        Objects.requireNonNull(engine);

        try {
            prepareConnect(engine);
        } catch (Exception e) {
            LOG.error("exception was occurred while connect", e);
            engine.finish(false);
            return false;
        }

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
                } catch (TgsqlMessageException e) {
                    LOG.trace("message exception", e);
                    LOG.error(e.getMessage());
                    long time = e.getTimingTime();
                    if (time != 0) {
                        var clientVariableMap = engine.getConfig().getClientVariableMap();
                        boolean timing = clientVariableMap.get(TgsqlCvKey.SQL_TIMING, false);
                        if (timing) {
                            var reporter = engine.getReporter();
                            reporter.reportTiming(time);
                        }
                    }
                    engine.finish(false);
                    return false;
                } catch (TgsqlNoMessageException e) {
                    LOG.trace("no message exception", e);
                    engine.finish(false);
                    return false;
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
     * @param config          tgsql configuration
     * @param engineWrapper   the statement executor
     * @param resultProcessor result processor
     * @param reporter        reporter
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while establishing connection
     * @throws InterruptedException if interrupted while establishing connection
     */
    public static void repl(//
            @Nonnull StatementSupplier script, //
            @Nonnull TgsqlConfig config, //
            @Nonnull Function<AbstractEngine, Engine> engineWrapper, //
            @Nonnull ResultProcessor resultProcessor, //
            @Nonnull TgsqlReporter reporter) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(config);
        Objects.requireNonNull(script);
        Objects.requireNonNull(resultProcessor);

        try (var sqlProcessor = new BasicSqlProcessor()) {
            var engine = new BasicEngine(config, sqlProcessor, resultProcessor, reporter);
            repl(script, engineWrapper.apply(engine));
        }
    }

    /**
     * statement supplier.
     */
    @FunctionalInterface
    public interface StatementSupplier {
        /**
         * get statement.
         *
         * @param config      tgsql configuration
         * @param transaction transaction
         * @return list of statement
         * @throws IOException if I/O error was occurred
         */
        List<Statement> get(TgsqlConfig config, @Nullable TransactionWrapper transaction) throws IOException;
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
            @Nonnull StatementSupplier script, //
            @Nonnull Engine engine) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(script);
        Objects.requireNonNull(engine);

        try {
            prepareConnect(engine);
        } catch (Exception e) {
            LOG.trace("exception was occurred while connect", e);
            var reporter = engine.getReporter();
            reporter.warn(MessageFormat.format("{0} (Use `\\connect` to connect)", e.getMessage()));
        }

        LOG.info("start repl");
        loop: while (true) {
            try {
                var transaction = engine.getTransaction();
                List<Statement> statementList = script.get(engine.getConfig(), transaction);
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
            } catch (TgsqlInterruptedException e) {
                LOG.trace("user interrupted", e);
            } catch (TgsqlMessageException e) {
                LOG.trace("message exception", e);
                var reporter = engine.getReporter();
                reporter.warn(e.getMessage());
                long time = e.getTimingTime();
                if (time != 0) {
                    var clientVariableMap = engine.getConfig().getClientVariableMap();
                    boolean timing = clientVariableMap.get(TgsqlCvKey.SQL_TIMING, false);
                    if (timing) {
                        reporter.reportTiming(time);
                    }
                }
            } catch (TgsqlNoMessageException e) {
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

    private TgsqlRunner() {
        throw new AssertionError();
    }
}
