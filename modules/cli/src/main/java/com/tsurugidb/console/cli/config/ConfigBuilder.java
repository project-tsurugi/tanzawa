package com.tsurugidb.console.cli.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;
import com.tsurugidb.console.cli.argument.CliArgument;
import com.tsurugidb.console.cli.argument.CliArgument.TransactionEnum;
import com.tsurugidb.console.cli.repl.jline.ReplJLineReader;
import com.tsurugidb.console.core.config.ScriptClientVariableMap;
import com.tsurugidb.console.core.config.ScriptCommitMode;
import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.TransactionType;
import com.tsurugidb.sql.proto.SqlRequest.WritePreserve;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.FileCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

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
        switch (transaction) {
        case SHORT:
        case OCC:
            options.setType(TransactionType.SHORT);
            break;
        case LONG:
        case LTX:
            options.setType(TransactionType.LONG);
            break;
        case READ:
        case READONLY:
        case RO:
            options.setType(TransactionType.READ_ONLY);
            break;
        case MANUAL:
            log.debug("config.transactionOption=<manual>");
            config.setTransactionOption(null);
            return;
        }

        List<String> writePreserve = argument.getWritePreserve();
        for (var tableName : writePreserve) {
            var wp = WritePreserve.newBuilder().setTableName(tableName).build();
            options.addWritePreserves(wp);
        }

        // TODO List<String> readAreaInclude = argument.getReadAreaInclude();
        // TODO List<String> readAreaExclude = argument.getReadAreaExclude();
        // TODO List<String> execute = argument.getExecute();

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

        var variable = argument.getClientVariable();
        log.debug("config.clientVariable={}", variable);
        clientVariableMap.putAll(variable);
    }

    protected void fillClientVariableDefault(ScriptClientVariableMap clientVariableMap) {
        // do override
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
        var credential = getCredential();
        log.debug("config.credentialSupplier={}", credential);
        config.setCredentialSupplier(credential);
    }

    private Supplier<Credential> getCredential() {
        var credentialList = argument.getCredentialList();
        switch (credentialList.size()) {
        case 0:
            return this::getDefaultCredential;
        case 1:
            return credentialList.get(0);
        default:
            throw new ParameterException("specify only one of [--user, --auth-token, --credentials, --no-auth]");
        }
    }

    private Credential getDefaultCredential() {
        // 1. 環境変数TSURUGI_AUTH_TOKEN
        Optional<String> authToken = CliEnvironment.findTsurugiAuthToken();
        boolean hasAuthToken = authToken.isPresent();
        log.trace("default credential 1. env.TSURUGI_AUTH_TOKEN={}", hasAuthToken ? "<not null>" : "null");
        if (hasAuthToken) {
            return new RememberMeCredential(authToken.get());
        }

        // 2. 既定の認証情報ファイル
        Optional<Path> credentialPath = CliEnvironment.findUserHomeCredentialPath();
        if (credentialPath.isEmpty()) {
            log.trace("default credential 2. user.home=null");
        } else {
            var path = credentialPath.get();
            boolean exists = Files.exists(path);
            log.trace("default credential 2. path={}, exists={}", path, exists);
            if (exists) {
                try {
                    return FileCredential.load(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }

        // 3. ユーザー入力
        String user = readUser();
        log.trace("default credential 3. user=[{}]", user);
        if (!user.isEmpty()) {
            String password = readPassword();
            return new UsernamePasswordCredential(user, password);
        }

        // 4. 認証なし
        log.trace("default credential 4. no auth");
        return NullCredential.INSTANCE;
    }

    /**
     * get user from console.
     *
     * @return user
     */
    @Nonnull
    public static String readUser() {
        var lineReader = ReplJLineReader.createSimpleReader();
        return lineReader.readLine("user: ");
    }

    /**
     * get password from console.
     *
     * @return password
     */
    @Nonnull
    public static String readPassword() {
        var lineReader = ReplJLineReader.createSimpleReader();
        return lineReader.readLine("password: ", '*');
    }
}
