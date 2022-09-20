package com.tsurugidb.console.cli.config;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;
import com.tsurugidb.console.cli.argument.CommonArgument;
import com.tsurugidb.console.cli.argument.CommonArgument.TransactionEnum;
import com.tsurugidb.console.cli.repl.jline.ReplJLineReader;
import com.tsurugidb.console.core.config.ScriptCommitMode;
import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.TransactionType;
import com.tsurugidb.sql.proto.SqlRequest.WritePreserve;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

/**
 * Configuration builder.
 *
 * @param <A> argument
 */
public abstract class ConfigBuilder<A extends CommonArgument> {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final ScriptConfig config = new ScriptConfig();
    protected final A argument;

    /**
     * Creates a new instance.
     * 
     * @param argument argument
     */
    public ConfigBuilder(A argument) {
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

        buildSub();

        fillCredential();
        return this.config;
    }

    private void fillEndpoint() {
        URI endpoint;
        try {
            endpoint = URI.create(argument.getConnectionUri());
        } catch (Exception e) {
            throw new RuntimeException("invalid connection-uri", e);
        }

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

    protected abstract void buildSub();

    protected void fillCommitMode(Boolean autoCommit, Boolean noAutoCommit, Boolean commit, Boolean noCommit, ScriptCommitMode defaultMode, Supplier<String> errorMessage) {
        var list = new ArrayList<ScriptCommitMode>();
        if (autoCommit != null && autoCommit) {
            list.add(ScriptCommitMode.AUTO_COMMIT);
        }
        if (noAutoCommit != null && noAutoCommit) {
            list.add(ScriptCommitMode.NO_AUTO_COMMIT);
        }
        if (commit != null && commit) {
            list.add(ScriptCommitMode.COMMIT);
        }
        if (noCommit != null && noCommit) {
            list.add(ScriptCommitMode.NO_COMMIT);
        }

        ScriptCommitMode commitMode;
        switch (list.size()) {
        case 0:
            commitMode = defaultMode;
            break;
        case 1:
            commitMode = list.get(0);
            break;
        default:
            String message = errorMessage.get();
            throw new ParameterException(MessageFormat.format("specify only one of {0}", message));
        }

        log.debug("config.commitMode={}", commitMode);
        config.setCommitMode(commitMode);
    }

    /*
     * credential
     */

    private void fillCredential() {
        var credential = getCredential();
        log.debug("config.credential={}", credential);
        config.setCredential(credential);
    }

    private Credential getCredential() {
        var credentialList = argument.getCredentialList();
        switch (credentialList.size()) {
        case 0:
            return getDefaultCredential();
        case 1:
            Supplier<Credential> supplier = credentialList.get(0);
            return supplier.get();
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
//            if (exists) {
            // TODO return FileCredential.load(path);
//            }
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
