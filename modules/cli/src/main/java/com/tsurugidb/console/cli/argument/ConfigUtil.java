package com.tsurugidb.console.cli.argument;

import java.net.URI;
import java.text.MessageFormat;
import java.util.List;

import com.beust.jcommander.ParameterException;
import com.tsurugidb.console.cli.argument.CommonArgument.TransactionEnum;
import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.TransactionType;
import com.tsurugidb.sql.proto.SqlRequest.WritePreserve;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;

public final class ConfigUtil {

    public static void fillConsoleConfig(ScriptConfig config, ConsoleArgument argument) {
        var util = new ConfigUtil(config);
        util.fillCommonConfig(argument);

        // TODO Auto-generated method stub
    }

    public static void fillExecConfig(ScriptConfig config, ExecArgument argument) {
        var util = new ConfigUtil(config);
        util.fillCommonConfig(argument);

        // TODO Auto-generated method stub
    }

    public static void fillScriptConfig(ScriptConfig config, ScriptArgument argument) {
        var util = new ConfigUtil(config);
        util.fillCommonConfig(argument);

        // TODO Auto-generated method stub
    }

    private ScriptConfig config;

    private ConfigUtil(ScriptConfig config) {
        this.config = config;
    }

    // common

    public void fillCommonConfig(CommonArgument argument) {
        processJavaProperty(argument);
        fillEndpoint(argument);
        fillCredential(argument);
        fillTransactionOption(argument);
        fillProperty(argument);
    }

    private void processJavaProperty(CommonArgument argument) {
        var list = argument.getJavaPropertyList();
        for (String s : list) {
            var kv = new KeyValue(s);
            System.setProperty(kv.getKey(), kv.getValue());
        }
    }

    private static class KeyValue {

        private String key;
        private String value;

        KeyValue(String s) {
            int n = s.indexOf('=');
            if (n < 0) {
                throw new ParameterException(MessageFormat.format("not <key>=<value>. arg=[{0}]", s));
            }
            this.key = s.substring(0, n).trim();
            this.value = s.substring(n + 1);
        }

        public String getKey() {
            return this.key;
        }

        public String getValue() {
            return this.value;
        }
    }

    private void fillEndpoint(CommonArgument argument) {
        var endpoint = URI.create(argument.getConnectionUri());
        config.setEndpoint(endpoint);
    }

    private void fillCredential(CommonArgument argument) {
        var credentialList = argument.getCredentialList();
        switch (credentialList.size()) {
        case 0: // TODO default credential
            // https://github.com/project-tsurugi/tateyama/blob/master/docs/cli-spec-ja.md#%E8%AA%8D%E8%A8%BC%E3%82%AA%E3%83%97%E3%82%B7%E3%83%A7%E3%83%B3
            config.setCredential(NullCredential.INSTANCE);
            break;
        case 1:
            var supplier = credentialList.get(0);
            var credential = supplier.get();
            config.setCredential(credential);
            break;
        default:
            throw new ParameterException("specify only one credential parameter");
        }
    }

    private void fillTransactionOption(CommonArgument argument) {
        var options = SqlRequest.TransactionOption.newBuilder();
        options.setLabel("default-transaction");

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

        config.setTransactionOption(options.build());
    }

    private void fillProperty(CommonArgument argument) {
        // TODO propertyList
        argument.getPropertyList();
    }
}
