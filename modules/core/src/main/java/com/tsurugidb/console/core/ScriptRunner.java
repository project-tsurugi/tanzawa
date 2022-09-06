package com.tsurugidb.console.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.executor.BasicEngine;
import com.tsurugidb.console.core.executor.BasicResultProcessor;
import com.tsurugidb.console.core.executor.BasicSqlProcessor;
import com.tsurugidb.console.core.executor.Engine;
import com.tsurugidb.console.core.executor.IoSupplier;
import com.tsurugidb.console.core.model.Statement;
import com.tsurugidb.console.core.parser.SqlParser;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;

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
        var endpoint = URI.create(args[1]);
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
            @Nonnull URI endpoint, //
            @Nonnull Credential credential) throws ServerException, IOException, InterruptedException {
        Objects.requireNonNull(script);
        Objects.requireNonNull(endpoint);
        Objects.requireNonNull(credential);
        LOG.info("establishing connection: {}", endpoint);
        try (var session = SessionBuilder.connect(endpoint).withCredential(credential).create();
                var sqlProcessor = new BasicSqlProcessor(SqlClient.attach(session));
                var resultProcessor = new BasicResultProcessor()) {
            return execute(script, new BasicEngine(sqlProcessor, resultProcessor));
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
        LOG.info("start processing script");
        try (var parser = new SqlParser(script.get())) {
            while (true) {
                Statement statement = parser.next();
                if (statement == null) {
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
                    return false;
                }
            }
        }
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

    public static void repl(Reader reader, Engine engine) throws IOException {
        try (var parser = new SqlParser(reader)) {
            while (true) {
                try {
                    Statement statement = parser.next();
                    if (statement == null) {
                        break;
                    }
                    boolean cont = engine.execute(statement);
                    if (!cont) {
                        LOG.info("shutdown was requested");
                        break;
                    }
                } catch (ServerException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.warn("{}", e.getDiagnosticCode().name(), e);
                    } else {
                        LOG.warn("{} ({})", e.getDiagnosticCode().name(), e.getMessage());
                    }
                } catch (Exception e) {
                    if (e.getMessage() != null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.warn("{}", e.getMessage(), e);
                        } else {
                            LOG.warn("{}", e.getMessage());
                        }
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.warn("{}", e.getClass().getName(), e);
                        } else {
                            LOG.warn("{}", e.getClass().getName());
                        }
                    }
                }
            }
        }
        LOG.info("repl execution was successfully completed");
    }

    private ScriptRunner() {
        throw new AssertionError();
    }
}
