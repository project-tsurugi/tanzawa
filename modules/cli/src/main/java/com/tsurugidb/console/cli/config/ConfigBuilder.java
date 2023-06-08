package com.tsurugidb.console.cli.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jline.utils.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;
import com.tsurugidb.console.cli.argument.CliArgument;
import com.tsurugidb.console.cli.argument.CliArgument.TransactionEnum;
import com.tsurugidb.console.cli.repl.ReplCredentialSupplier;
import com.tsurugidb.console.core.config.ScriptClientVariableMap;
import com.tsurugidb.console.core.config.ScriptCommitMode;
import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.credential.CredentialDefaultSupplier;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.ReadArea;
import com.tsurugidb.sql.proto.SqlRequest.TransactionPriority;
import com.tsurugidb.sql.proto.SqlRequest.WritePreserve;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;

/**
 * Configuration builder.
 *
 * @param <A> argument
 */
public abstract class ConfigBuilder {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final ScriptConfig config = new ScriptConfig();
    protected final CliArgument argument;

    /**
     * Creates a new instance.
     *
     * @param argument argument
     */
    public ConfigBuilder(CliArgument argument) {
        this.argument = argument;
    }

    /**
     * create configuration.
     *
     * @return script configuration
     */
    public ScriptConfig build() {
        fillEndpoint();
        fillTransactionOption();
        fillProperty();
        fillClientVariable();

        buildSub();

        fillCredential();
        return this.config;
    }

    private void fillEndpoint() {
        String endpoint = argument.getConnectionUri();
        log.debug("config.endpoint={}", endpoint);
        config.setEndpoint(endpoint);
    }

    private void fillTransactionOption() {
        var options = SqlRequest.TransactionOption.newBuilder();

        TransactionEnum transaction = argument.getTransaction();
        var type = transaction.toTransactionType();
        if (type != null) {
            options.setType(type);
        } else { // manual
            log.debug("config.transactionOption=<manual>");
            config.setTransactionOption(null);
            return;
        }

        List<String> writePreserve = argument.getWritePreserve();
        for (var tableName : writePreserve) {
            var wp = WritePreserve.newBuilder().setTableName(tableName).build();
            options.addWritePreserves(wp);
        }

        List<String> readAreaInclude = argument.getReadAreaInclude();
        for (var tableName : readAreaInclude) {
            var area = ReadArea.newBuilder().setTableName(tableName).build();
            options.addInclusiveReadAreas(area);
        }
        List<String> readAreaExclude = argument.getReadAreaExclude();
        for (var tableName : readAreaExclude) {
            var area = ReadArea.newBuilder().setTableName(tableName).build();
            options.addExclusiveReadAreas(area);
        }

        var execute = argument.getExecute();
        if (execute != null) {
            TransactionPriority prior;
            if (execute.isDeferrable()) {
                if (execute.isExcluding()) {
                    prior = TransactionPriority.WAIT_EXCLUDE;
                } else {
                    prior = TransactionPriority.WAIT;
                }
            } else {
                if (execute.isExcluding()) {
                    prior = TransactionPriority.INTERRUPT_EXCLUDE;
                } else {
                    prior = TransactionPriority.INTERRUPT;
                }
            }
            options.setPriority(prior);
        }

        String label = argument.getLabel();
        options.setLabel(label);

        // TODO Map<String, String> with = argument.getWith();

        var option = options.build();
        log.debug("config.transactionOption={}", option);
        config.setTransactionOption(option);
    }

    private void fillProperty() {
        var property = argument.getProperty();
        log.debug("config.property={}", property);
        config.setProperty(property);
    }

    private void fillClientVariable() {
        var clientVariableMap = config.getClientVariableMap();
        fillClientVariableDefault(clientVariableMap);
        fillClientVariableFromUserHomeFile(clientVariableMap);
        fillClientVariableFromArgumentFile(clientVariableMap);

        var variable = argument.getClientVariable();
        log.debug("config.clientVariable={}", variable);
        clientVariableMap.putAll(variable);
    }

    protected void fillClientVariableDefault(ScriptClientVariableMap clientVariableMap) {
        // do override
    }

    protected void fillClientVariableFromUserHomeFile(ScriptClientVariableMap clientVariableMap) {
        CliEnvironment.findUserHomeClientVariablePath().ifPresent(path -> {
            fillClientVariableFromFile(clientVariableMap, path, true);
        });
    }

    protected void fillClientVariableFromArgumentFile(ScriptClientVariableMap clientVariableMap) {
        String file = argument.getClientVariableFile();
        if (file != null) {
            var path = Path.of(file);
            fillClientVariableFromFile(clientVariableMap, path, false);
        }
    }

    protected void fillClientVariableFromFile(ScriptClientVariableMap clientVariableMap, Path file, boolean ignoreError) {
        if (ignoreError) {
            if (!Files.isRegularFile(file)) {
                Log.debug("{} is not regular file. ignore", file);
                return;
            }
        }
        var properties = new Properties();
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            if (ignoreError) {
                Log.debug("client-variable file read error. ignore", e);
                return;
            }
            String message;
            if (e instanceof NoSuchFileException) {
                message = MessageFormat.format("file not found. {0}", e.getMessage());
            } else {
                message = e.getMessage();
            }
            throw new UncheckedIOException(message, e);
        }
        properties.forEach((key, value) -> {
            try {
                clientVariableMap.put((String) key, (String) value);
            } catch (Exception e) {
                String message = MessageFormat.format("property error. file={0}, key={1}, value={2}. {3}", file, key, value, e.getMessage());
                throw new RuntimeException(message, e);
            }
        });
    }

    protected abstract void buildSub();

    protected void fillCommitMode(Set<ScriptCommitMode> availableList, ScriptCommitMode defaultMode) {
        var list = new ArrayList<ScriptCommitMode>();
        boolean error = false;
        error |= computeCommitMode(list, availableList, ScriptCommitMode.AUTO_COMMIT, argument.getAutoCommit());
        error |= computeCommitMode(list, availableList, ScriptCommitMode.NO_AUTO_COMMIT, argument.getNoAutoCommit());
        error |= computeCommitMode(list, availableList, ScriptCommitMode.COMMIT, argument.getCommit());
        error |= computeCommitMode(list, availableList, ScriptCommitMode.NO_COMMIT, argument.getNoCommit());

        ScriptCommitMode commitMode;
        switch (list.size()) {
        case 0:
            commitMode = defaultMode;
            break;
        case 1:
            commitMode = list.get(0);
            break;
        default:
            commitMode = null;
            break;
        }
        if (commitMode == null || error) {
            String message = availableList.stream().map(mode -> "--" + mode.name().toLowerCase().replace('_', '-')).collect(Collectors.joining(", ", "[", "]"));
            throw new ParameterException(MessageFormat.format("specify only one of {0}", message));
        }

        log.debug("config.commitMode={}", commitMode);
        config.setCommitMode(commitMode);
    }

    private boolean computeCommitMode(List<ScriptCommitMode> list, Set<ScriptCommitMode> availableList, ScriptCommitMode mode, boolean b) {
        if (b) {
            if (availableList.contains(mode)) {
                list.add(mode);
                return false;
            }
            return true; // error
        }
        return false;
    }

    /*
     * credential
     */

    private void fillCredential() {
        var defaultCredentialSupplier = createDefaultCredentialSupplier();
        config.setDefaultCredentialSupplier(defaultCredentialSupplier);
        log.debug("config.defaultCredentialSupplier={}", defaultCredentialSupplier);

        var credential = getCredential(defaultCredentialSupplier);
        log.debug("config.credentialSupplier={}", credential);
        config.setCredentialSupplier(credential);
    }

    protected CredentialDefaultSupplier createDefaultCredentialSupplier() {
        return new ReplCredentialSupplier();
    }

    private Supplier<Credential> getCredential(CredentialDefaultSupplier defaultCredentialSupplier) {
        var credentialList = argument.getCredentialList();
        switch (credentialList.size()) {
        case 0:
            return defaultCredentialSupplier::getDefaultCredential;
        case 1:
            return credentialList.get(0);
        default:
            throw new ParameterException("specify only one of [--user, --auth-token, --credentials, --no-auth]");
        }
    }
}
