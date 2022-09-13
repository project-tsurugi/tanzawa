package com.tsurugidb.console.cli.argument;

import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.ParameterException;
import com.tsurugidb.console.cli.argument.CommonArgument.TransactionEnum;
import com.tsurugidb.console.core.config.ScriptCommitMode;
import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.TransactionType;
import com.tsurugidb.sql.proto.SqlRequest.WritePreserve;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;

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

        LOG.debug("endpoint={}", endpoint);
        config.setEndpoint(endpoint);
    }

    private void fillCredential(CommonArgument argument) {
        Credential credential;
        var credentialList = argument.getCredentialList();
        switch (credentialList.size()) {
        case 0:
            // https://github.com/project-tsurugi/tateyama/blob/master/docs/cli-spec-ja.md#%E8%AA%8D%E8%A8%BC%E3%82%AA%E3%83%97%E3%82%B7%E3%83%A7%E3%83%B3
            credential = NullCredential.INSTANCE; // TODO default credential
            break;
        case 1:
            var supplier = credentialList.get(0);
            credential = supplier.get();
            break;
        default:
            throw new ParameterException("specify only one credential parameter");
        }

        LOG.debug("credential={}", credential);
        config.setCredential(credential);
    }

    private void fillTransactionOption(CommonArgument argument) {
        var options = SqlRequest.TransactionOption.newBuilder();
        options.setLabel("tgsql-transaction");

        TransactionEnum transaction = argument.getTransaction();
        switch (transaction) {
        case SHORT:
        case OCC:
            options.setType(TransactionType.SHORT);
            break;
        case LONG:
        case LTX:
            options.setType(TransactionType.LONG);
            List<String> tableList = argument.getWritePreserve();
            for (var tableName : tableList) {
                var wp = WritePreserve.newBuilder().setTableName(tableName).build();
                options.addWritePreserves(wp);
            }
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

        var option = options.build();
        LOG.debug("transactionOption={}", option);
        config.setTransactionOption(option);
    }

    private void fillProperty(CommonArgument argument) {
        // TODO propertyList
        argument.getPropertyList();
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

        LOG.debug("commitMode={}", commitMode);
        config.setCommitMode(commitMode);
    }
}
