package com.tsurugidb.console.cli.argument;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;
import com.tsurugidb.console.cli.argument.CommonArgument.TransactionEnum;
import com.tsurugidb.console.cli.repl.ReplLineReader;
import com.tsurugidb.console.core.config.ScriptCommitMode;
import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.TransactionType;
import com.tsurugidb.sql.proto.SqlRequest.WritePreserve;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

public final class ConfigUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigUtil.class);

    public static ScriptConfig createConsoleConfig(ConsoleArgument argument) {
        var config = new ScriptConfig();
        var util = new ConfigUtil(config);
        util.fillCommonConfig(argument);

        util.fillCommitMode(//
                argument.getAutoCommit(), argument.getNoAutoCommit(), //
                null, null, //
                ScriptCommitMode.NO_AUTO_COMMIT, //
                () -> List.of("--auto-commit", "--no-auto-commit").toString());

        return config;
    }

    public static ScriptConfig createExecConfig(ExecArgument argument) {
        var config = new ScriptConfig();
        var util = new ConfigUtil(config);
        util.fillCommonConfig(argument);

        util.fillCommitMode(//
                null, null, //
                argument.getCommit(), argument.getNoCommit(), //
                ScriptCommitMode.COMMIT, //
                () -> List.of("--commit", "--no-commit").toString());

        return config;
    }

    public static ScriptConfig createScriptConfig(ScriptArgument argument) {
        var config = new ScriptConfig();
        var util = new ConfigUtil(config);
        util.fillCommonConfig(argument);

        util.fillCommitMode(//
                argument.getAutoCommit(), argument.getNoAutoCommit(), //
                argument.getCommit(), argument.getNoCommit(), //
                ScriptCommitMode.COMMIT, //
                () -> List.of("--auto-commit", "--no-auto-commit", "--commit", "--no-commit").toString());

        return config;
    }

    private ScriptConfig config;

    private ConfigUtil(ScriptConfig config) {
        this.config = config;
    }

    public void fillCommonConfig(CommonArgument argument) {
        fillEndpoint(argument);
        fillCredential(argument);
        fillTransactionOption(argument);
        fillProperty(argument);
    }

    private void fillEndpoint(CommonArgument argument) {
        URI endpoint;
        try {
            endpoint = URI.create(argument.getConnectionUri());
        } catch (Exception e) {
            throw new RuntimeException("invalid connection-uri", e);
        }

        LOG.debug("config.endpoint={}", endpoint);
        config.setEndpoint(endpoint);
    }

    private void fillCredential(CommonArgument argument) {
        Credential credential;
        var credentialList = argument.getCredentialList();
        switch (credentialList.size()) {
        case 0:
            credential = getDefaultCredential();
            break;
        case 1:
            Supplier<Credential> supplier = credentialList.get(0);
            credential = supplier.get();
            break;
        default:
            throw new ParameterException("specify only one of [--user, --auth-token, --credentials, --no-auth]");
        }

        LOG.debug("config.credential={}", credential);
        config.setCredential(credential);
    }

    private Credential getDefaultCredential() {
        // 1. 環境変数TSURUGI_AUTH_TOKEN
        String authToken = System.getenv("TSURUGI_AUTH_TOKEN");
        boolean hasAuthToken = (authToken != null);
        LOG.trace("default credential 1. env.TSURUGI_AUTH_TOKEN={}", hasAuthToken ? "<not null>" : "null");
        if (hasAuthToken) {
            return new RememberMeCredential(authToken);
        }

        // 2. 既定の認証情報ファイル
        String home = System.getProperty("user.home");
        if (home == null) {
            LOG.trace("default credential 2. user.home=null");
        } else {
            var path = Paths.get(home, ".tsurugidb/credentials.json");
            boolean exists = Files.exists(path);
            LOG.trace("default credential 2. path={}, exists={}", path, exists);
//            if (exists) {
            // TODO return FileCredential.load(path);
//            }
        }

        // 3. ユーザー入力
        String user = readUser();
        LOG.trace("default credential 3. user=[{}]", user);
        if (!user.isEmpty()) {
            String password = readPassword();
            return new UsernamePasswordCredential(user, password);
        }

        // 4. 認証なし
        LOG.trace("default credential 4. no auth");
        return NullCredential.INSTANCE;
    }

    @Nonnull
    public static String readUser() {
        var lineReader = ReplLineReader.createSimpleReader();
        return lineReader.readLine("user: ");
    }

    @Nonnull
    public static String readPassword() {
        var lineReader = ReplLineReader.createSimpleReader();
        return lineReader.readLine("password: ", '*');
    }

    private void fillTransactionOption(CommonArgument argument) {
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
        LOG.debug("config.transactionOption={}", option);
        config.setTransactionOption(option);
    }

    private void fillProperty(CommonArgument argument) {
        var property = argument.getProperty();
        LOG.debug("config.property={}", property);
        config.setProperty(property);
    }

    private void fillCommitMode(Boolean autoCommit, Boolean noAutoCommit, Boolean commit, Boolean noCommit, ScriptCommitMode defaultMode, Supplier<String> errorMessage) {
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

        LOG.debug("config.commitMode={}", commitMode);
        config.setCommitMode(commitMode);
    }
}
