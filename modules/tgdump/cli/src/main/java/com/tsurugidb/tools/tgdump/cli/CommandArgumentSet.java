/*
 * Copyright 2023-2024 Project Tsurugi.
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
package com.tsurugidb.tools.tgdump.cli;

import java.net.URI;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.tsurugidb.tools.common.connection.ConnectionProvider;
import com.tsurugidb.tools.tgdump.core.engine.DumpTargetSelector;
import com.tsurugidb.tools.tgdump.core.engine.QueryDumpTargetSelector;
import com.tsurugidb.tools.tgdump.core.engine.TableDumpTargetSelector;
import com.tsurugidb.tools.tgdump.core.model.TransactionSettings;
import com.tsurugidb.tools.tgdump.profile.DumpProfileBundleLoader;
import com.tsurugidb.tools.tgdump.profile.DumpProfileReader;

/**
 * A parameter set of Tsurugi Dump Tools ({@literal a.k.a.} {@code tgdump}}) command.
 */
public class CommandArgumentSet {

    /**
     * A validator to restrict empty names.
     */
    public static final class NoEmptyElementValidator implements IParameterValidator {
        @Override
        public void validate(String name, String value) throws ParameterException {
            if (value.isEmpty()) {
                throw new ParameterException(MessageFormat.format(
                        "\"{0}\" must contain empty name",
                        name));
            }
        }
    }

    /**
     * A validator to ensure one or more parameter values.
     */
    public static class OneOrMoreValidator implements IValueValidator<Integer> {
        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value <= 0) {
                throw new ParameterException(MessageFormat.format(
                        "\"{0}\" must be >= 1 (specified: {1})",
                        name,
                        value));
            }
        }
    }

    /**
     * A validator to ensure valid endpoint URI.
     */
    public static class ConnectionUriValidator implements IValueValidator<URI> {
        @Override
        public void validate(String name, URI value) throws ParameterException {
            var schema = value.getScheme();
            if (!AVAILABLE_ENDPOINT_URI_SCHEMA.contains(schema)) {
                throw new ParameterException(MessageFormat.format(
                        "\"{0}\" schema must be one of {1} (specified: {2})",
                        name,
                        AVAILABLE_ENDPOINT_URI_SCHEMA.stream().sorted().collect(Collectors.toList()),
                        value));
            }
        }
    }

    /**
     * A convert to convert transaction types.
     */
    public static class TransactionTypeConverter implements IStringConverter<TransactionSettings.Type> {

        private final String optionName;

        /**
         * Creates a new instance.
         * @param optionName the option name.
         */
        public TransactionTypeConverter(String optionName) {
            this.optionName = optionName;
        }

        private static final Map<String, TransactionSettings.Type> NAMES = Map.of(
                // OCC
                "occ", TransactionSettings.Type.OCC,
                "short", TransactionSettings.Type.OCC,
                // LTX
                "ltx", TransactionSettings.Type.LTX,
                "long", TransactionSettings.Type.LTX,
                // RTX
                "rtx", DEFAULT_TRANSACTION_TYPE,
                "read", DEFAULT_TRANSACTION_TYPE,
                "readonly", DEFAULT_TRANSACTION_TYPE,
                "read-only", DEFAULT_TRANSACTION_TYPE);

        @Override
        public TransactionSettings.Type convert(String value) {
            var result = NAMES.get(value.toLowerCase(Locale.ENGLISH));
            if (result == null) {
                throw new ParameterException(MessageFormat.format(
                        "\"{1}\" ({0}) is not a valid transaction type. It must be one of '{'occ, ltx, rtx'}.'",
                        optionName,
                        value));
            }
            return result;
        }
    }

    /**
     * The default profile name.
     */
    public static final String DEFAULT_PROFILE = "default"; //$NON-NLS-1$

    /**
     * The default transaction type ({@link com.tsurugidb.tools.tgdump.core.model.TransactionSettings.Type#RTX RTX}).
     */
    public static final TransactionSettings.Type DEFAULT_TRANSACTION_TYPE = TransactionSettings.Type.RTX;

    /**
     * The default number of dump operation worker threads.
     */
    public static final int DEFAULT_NUMBER_OF_WORKER_THREADS = 1;

    /**
     * The available endpoint URI schemas.
     */
    public static final Set<String> AVAILABLE_ENDPOINT_URI_SCHEMA = Set.of("ipc");

    private static final Logger LOG = LoggerFactory.getLogger(CommandArgumentSet.class);

    // NOTE: cannot annotate setter method with a list parameter
    @Parameter(
            description = "<table-name-list>",
            validateWith = NoEmptyElementValidator.class,
            required = true)
    private List<String> tableNames;

    private boolean queryMode = false;

    private boolean singleMode = false;

    private Path destinationPath;

    private Path profile = Path.of(DEFAULT_PROFILE);

    private URI connectionUri;

    private String connectionLabel = null;

    private long connectionTimeoutMillis;

    private TransactionSettings.Type transactionType = DEFAULT_TRANSACTION_TYPE;

    private String transactionLabel = null;

    private int numberOfWorkerThreads = DEFAULT_NUMBER_OF_WORKER_THREADS;

    private boolean verbose = false;

    private Path monitorOutput = null;

    private boolean printHelp;

    private boolean printVersion;

    // non-configurable arguments

    private DumpProfileReader profileReader;

    private DumpProfileBundleLoader profileBundleLoader;

    private DumpTargetSelector targetSelector;

    private ConnectionProvider connectionProvider;

    /**
     * Returns the dump target table names.
     * @return the table name list
     * @see #isQueryMode()
     */
    public List<String> getTableNames() {
        if (tableNames == null) {
            return List.of();
        }
        return List.copyOf(tableNames);
    }

    /**
     * Sets the dump target table names.
     * @param nameList the table name list
     */
    public void setTableNames(@Nonnull List<String> nameList) {
        Objects.requireNonNull(nameList);
        LOG.trace("argument: <table-name>: {}", nameList); //$NON-NLS-1$
        this.tableNames = List.copyOf(nameList);
    }

    /**
     * Returns whether to specify query text instead of table names.
     * @return {@code true} if use query text, or {@code false} if use table names
     */
    public boolean isQueryMode() {
        return queryMode;
    }

    /**
     * Sets whether to specify query text instead of table names.
     * @param enable {@code true} to allow query text, {@code false} to use table names
     */
    @Parameter(
            order = 10,
            names = { "--sql" },
            arity = 0,
            description = "specify SQL text instead of table names",
            required = false)
    public void setQueryMode(boolean enable) {
        LOG.trace("argument: --sql: {}", enable); //$NON-NLS-1$
        this.queryMode = enable;
    }

    /**
     * Returns whether to output dump files into the {@link #getDestinationPath() destination directory} directly.
     * <p>
     * If this is {@code true}, the destination directory must contain only one {@link #getTableNames() table or query name}.
     * Otherwise, if this is {@code false}, the dump files will be placed into sub-directories for each table or query.
     * </p>
     * @return {@code true} if dump only a single table, {@code false} otherwise
     */
    public boolean isSingleMode() {
        return singleMode;
    }

    /**
     * Sets whether to output dump files into the {@link #getDestinationPath() destination directory} directly.
     * <p>
     * If this is {@code true}, the destination directory must contain only one {@link #getTableNames() table or query name}.
     * Otherwise, if this is {@code false}, the dump files will be placed into sub-directories for each table or query.
     * </p>
     * @param enable {@code true} to dump only a single table, {@code false} otherwise
     */
    @Parameter(
            order = 11,
            names = { "--single" },
            arity = 0,
            description = "Put dump files into the destination directory directly",
            required = false)
    public void setSingleMode(boolean enable) {
        LOG.trace("argument: --single: {}", enable); //$NON-NLS-1$
        this.singleMode = enable;
    }
    
    /**
     * Returns the dump files destination path.
     * @return the destination path
     */
    public Path getDestinationPath() {
        return destinationPath;
    }

    /**
     * Sets the dump files destination path.
     * @param path the destination path
     */
    @Parameter(
            order = 10,
            names = { "--to" },
            arity = 1,
            description = "Destination directory of dump files.",
            required = true)
    public void setDestinationPath(@Nonnull Path path) {
        Objects.requireNonNull(path);
        LOG.trace("argument: --to: {}", path); //$NON-NLS-1$
        this.destinationPath = path;
    }

    /**
     * Returns the dump profile path.
     * @return the dump profile path
     */
    public Path getProfile() {
        return profile;
    }

    /**
     * Sets the dump profile path.
     * @param path the dump profile path
     */
    @Parameter(
            order = 100,
            names = { "--profile" },
            arity = 1,
            description = "Dump profile name.",
            required = false)
    public void setProfile(@Nonnull Path path) {
        Objects.requireNonNull(path);
        LOG.trace("argument: --profile: {}", path); //$NON-NLS-1$
        this.profile = path;
    }

    /**
     * Returns the server end-point URI of the target tsurugidb.
     * @return the server end-point URI, or {@code null} if it is not set
     */
    public URI getConnectionUri() {
        return connectionUri;
    }

    /**
     * Sets the server end-point URI of the target tsurugidb.
     * @param uri the server end-point URI
     */
    @Parameter(
            order = 20,
            names = { "-c", "--connection" },
            arity = 1,
            description = "Tsurugi server endpoint URI.",
            validateValueWith = ConnectionUriValidator.class,
            required = true)
    public void setConnectionUri(@Nonnull URI uri) {
        Objects.requireNonNull(uri);
        LOG.trace("argument: --connection: {}", uri); //$NON-NLS-1$
        this.connectionUri = uri;
    }

    /**
     * Returns the connection label.
     * @return the connection label, or {@code null} if it is not specified
     */
    public String getConnectionLabel() {
        return connectionLabel;
    }

    /**
     * Sets the connection label.
     * @param label the connection label, or {@code null} to clear it
     */
    @Parameter(
            order = 21,
            names = { "--connection-label" },
            arity = 1,
            description = "Tsurugi connection session label.",
            required = false)
    public void setConnectionLabel(@Nullable String label) {
        LOG.trace("argument: --connection-label: {}", label); //$NON-NLS-1$
        this.connectionLabel = label;
    }

    /**
     * Returns the connection timeout in milliseconds.
     * @return the connection timeout in milliseconds, or {@code 0} to disable connection timeout
     */
    public long getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    /**
     * Sets  the connection timeout in milliseconds.
     * @param value  the connection timeout in milliseconds, or {@code 0} to disable connection timeout
     */
    @Parameter(
            order = 22,
            names = { "--connection-timeout" },
            arity = 1,
            description = "Connection timeout (in milliseconds).",
            required = false)
    public void setConnectionTimeoutMillis(long value) {
        if (value < 0) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "timeout must be >= 0 (specified: {0})",
                    value));
        }
        LOG.trace("argument: --connection-timeout: {}", value); //$NON-NLS-1$
        this.connectionTimeoutMillis = value;
    }

    /**
     * Returns the transaction type during dump operations.
     * @return the transaction type
     */
    public TransactionSettings.Type getTransactionType() {
        return transactionType;
    }

    /**
     * Sets the transaction type during dump operations.
     * @param type the transaction type
     */
    @Parameter(
            order = 110,
            names = { "--transaction" },
            arity = 1,
            description = "Transaction type.",
            converter = TransactionTypeConverter.class,
            required = false)
    public void setTransactionType(@Nonnull TransactionSettings.Type type) {
        Objects.requireNonNull(type);
        LOG.trace("argument: --transaction: {}", type); //$NON-NLS-1$
        this.transactionType = type;
    }

    /**
     * Returns the transaction label.
     * @return the transaction label, or {@code null} if it is not specified
     */
    public String getTransactionLabel() {
        return transactionLabel;
    }

    /**
     * Sets the transaction label.
     * @param label the transaction label, or {@code null} to clear it
     */
    @Parameter(
            order = 111,
            names = { "--transaction-label" },
            arity = 1,
            description = "Transaction label",
            required = false)
    public void setTransactionLabel(@Nullable String label) {
        LOG.trace("argument: --transaction-label: {}", label); //$NON-NLS-1$
        this.transactionLabel = label;
    }


    /**
     * Returns the number of dump operation threads.
     * @return the number of threads
     */
    public int getNumberOfWorkerThreads() {
        return numberOfWorkerThreads;
    }

    /**
     * Sets the number of dump operation threads.
     * @param count the number of threads
     * @throws IllegalArgumentException if the value is less than {@code 1}
     */
    @Parameter(
            order = 200,
            names = { "--threads" },
            arity = 1,
            description = "The number of dump operation threads",
            validateValueWith = OneOrMoreValidator.class,
            required = false)
    public void setNumberOfWorkerThreads(int count) {
        if (count < 1) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "the number of worker threads must be > 1 (specified: {0})",
                    count));
        }
        LOG.trace("argument: --threads: {}", count); //$NON-NLS-1$
        this.numberOfWorkerThreads = count;
    }

    /**
     * Returns whether or not to enable verbose output.
     * @return {@code true} if enable verbose output, {@code false} otherwise
     */
    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Sets whether or not to enable verbose output.
     * @param enable {@code true} to enable verbose output, {@code false} otherwise
     */
    @Parameter(
            order = 1000,
            names = { "-v", "--verbose" },
            arity = 0,
            description = "Enables verbose output messages",
            required = false)
    public void setVerbose(boolean enable) {
        LOG.trace("argument: --verbose: {}", enable); //$NON-NLS-1$
        this.verbose = enable;
    }

    /**
     * Returns the output path of monitoring information.
     * @return the monitoring information output
     */
    public Path getMonitorOutputPath() {
        return monitorOutput;
    }

    /**
     * Sets the output path of monitoring information.
     * @param path the monitoring information output
     */
    @Parameter(
            names = { "--monitor" },
            arity = 1,
            description = "Monitoring information destination file",
            hidden = true,
            required = false)
    public void setMonitorOutputPath(@Nonnull Path path) {
        Objects.requireNonNull(path);
        LOG.trace("argument: --monitor: {}", path); //$NON-NLS-1$
        this.monitorOutput = path;
    }

    /**
     * Returns whether or not to show the command help.
     * @return {@code true} to show the command help, {@code false} otherwise
     */
    public boolean isPrintHelp() {
        return printHelp;
    }

    /**
     * Sets whether or not to show the command help.
     * @param enable {@code true} to show the command help, {@code false} otherwise
     */
    @Parameter(
            order = 10000,
            names = { "-h", "--help" },
            arity = 0,
            description = "Print command help",
            help = true)
    public void setPrintHelp(boolean enable) {
        LOG.trace("argument: --help: {}", enable); //$NON-NLS-1$
        this.printHelp = enable;
    }

    /**
     * Returns whether or not to show the major library versions.
     * @return {@code true} to show the versions, {@code false} otherwise
     */
    public boolean isPrintVersion() {
        return printVersion;
    }

    /**
     * Sets whether or not to show the major library version.
     * @param enable {@code true} to show the versions, {@code false} otherwise
     */
    @Parameter(
            order = 10001,
            names = { "--version" },
            arity = 0,
            description = "Print library versions",
            help = true)
    public void setPrintVersion(boolean enable) {
        LOG.trace("argument: --version: {}", enable); //$NON-NLS-1$
        this.printVersion = enable;
    }

    /**
     * Returns the dump profile reader.
     *
     * <p>
     * This is designed only for tests, and may be removed later versions.
     * </p>
     * @return the dump profile reader
     */
    protected DumpProfileReader getProfileReader() {
        if (profileReader != null) {
            return profileReader;
        }
        return new DumpProfileReader();
    }

    /**
     * Sets the dump profile reader.
     *
     * <p>
     * This is designed only for tests, and may be removed later versions.
     * </p>
     * @param value the value to set, or {@code null} to set it to default
     */
    protected void setProfileReader(@Nullable DumpProfileReader value) {
        this.profileReader = value;
    }

    /**
     * Returns the built-in dump profile bundle loader.
     *
     * <p>
     * This is designed only for tests, and may be removed later versions.
     * </p>
     * @return the dump profile bundle loader
     */
    protected DumpProfileBundleLoader getProfileBundleLoader() {
        if (profileBundleLoader != null) {
            return profileBundleLoader;
        }
        return new DumpProfileBundleLoader(
            getProfileReader(),
            CommandArgumentSet.class.getClassLoader(),
            true);
    }

    /**
     * Sets the built-in dump profile bundle loader.
     *
     * <p>
     * This is designed only for tests, and may be removed later versions.
     * </p>
     * @param value the value to set, or {@code null} to set it to default
     */
    protected void setProfileBundleLoader(@Nullable DumpProfileBundleLoader value) {
        this.profileBundleLoader = value;
    }

    /**
     * Returns the dump target selector.
     *
     * <p>
     * This is designed only for tests, and may be removed later versions.
     * </p>
     * @return the dump target selector
     */
    protected DumpTargetSelector getTargetSelector() {
        if (targetSelector != null) {
            return this.targetSelector;
        }
        if (queryMode) {
            return new QueryDumpTargetSelector();
        }
        return new TableDumpTargetSelector();
    }

    /**
     * Sets the dump target selector.
     *
     * <p>
     * This is designed only for tests, and may be removed later versions.
     * </p>
     * @param value the value to set, or {@code null} to set it to default
     */
    protected void setTargetSelector(@Nullable DumpTargetSelector value) {
        this.targetSelector = value;
    }

    /**
     * Sets the connection provider.
     *
     * <p>
     * This is designed only for tests, and may be removed later versions.
     * </p>
     * @return the connection provider
     */
    protected ConnectionProvider getConnectionProvider() {
        if (connectionProvider != null) {
            return connectionProvider;
        }
        return new ConnectionProvider();
    }

    /**
     * Returns the connection provider.
     *
     * <p>
     * This is designed only for tests, and may be removed later versions.
     * </p>
     * @param value the value to set, or {@code null} to set it to default
     */
    protected void setConnectionProvider(@Nullable ConnectionProvider value) {
        this.connectionProvider = value;
    }

    /**
     * Validates the combination of the command arguments.
     * @throws ParameterException if this command arguments contain invalid combinations
     */
    public void validateCombination() {
        if (singleMode && tableNames.size() > 1) {
            if (queryMode) {
                throw new ParameterException("Cannot specify multiple queries with --single.");
            }
            throw new ParameterException("Cannot specify multiple table names with --single.");
        }
    }
}
