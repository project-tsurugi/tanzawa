package com.tsurugidb.console.cli.argument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.CommaParameterSplitter;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

@Parameters
public class CommonArgument {

    @Parameter(names = { "--connection", "-c" }, arity = 1, description = "connection uri (e.g. tcp://localhost:12345)", required = true)
    private String connectionUri;

    @DynamicParameter(names = { "--property", "-P" }, description = "SQL setting. <key>=<value>")
    private Map<String, String> propertyMap = new LinkedHashMap<>();

    /*
     * transaction
     */

    public enum TransactionEnum {
        SHORT("short"), OCC("OCC"), //
        LONG("long"), LTX("LTX"), //
        READ("read"), READONLY("readonly"), RO("RO"), //
        MANUAL("manual");

        private final String value;

        TransactionEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    @Parameter(names = { "--transaction", "-t" }, arity = 1, description = "transaction mode")
    private TransactionEnum transaction = TransactionEnum.OCC;

    @Parameter(names = { "--write-preserve", "-w" }, variableArity = true, description = "write preserve. <table>[,<table>[,...]]", splitter = CommaParameterSplitter.class)
    private List<String> writePreserve;

    @Parameter(names = { "--read-area-include" }, arity = 1, description = "read area include. <table>[,<table>[,...]]", splitter = CommaParameterSplitter.class)
    private List<String> readAreaInclude;

    @Parameter(names = { "--read-area-exclude" }, arity = 1, description = "read area exclude. <table>[,<table>[,...]]", splitter = CommaParameterSplitter.class)
    private List<String> readAreaExclude;

    @Parameter(names = { "--execute" }, variableArity = true, description = "transaction execute. (PRIOR|EXCLUDING) (DEFERRABLE|IMMEDIATE)?")
    private List<String> execute;

    @Parameter(names = { "--label", "-l" }, arity = 1, description = "transaction label")
    private String label = "tgsql-transaction";

    @DynamicParameter(names = { "--with" }, description = "transaction setting. <key>=<value>")
    private Map<String, String> withMap = new LinkedHashMap<>();

    /*
     * credential
     */

    @Parameter(names = { "--user", "-u" }, arity = 1, description = "<user name>")
    private String user;

    @Parameter(names = { "--auth-token" }, arity = 1, description = "<token>")
    private String authToken;

    @Parameter(names = { "--credentials" }, arity = 1, description = "</path/to/credentials.json>")
    private String credentials;

    @Parameter(names = { "--no-auth" }, arity = 0, description = "no auth")
    private Boolean noAuth;

    @Nonnull // required = true
    public String getConnectionUri() {
        return this.connectionUri;
    }

    @Nonnull
    public Map<String, String> getProperty() {
        return this.propertyMap;
    }

    @Nonnull // has default value
    public TransactionEnum getTransaction() {
        return this.transaction;
    }

    @Nonnull
    public List<String> getWritePreserve() {
        if (this.writePreserve == null) {
            return List.of();
        }
        return this.writePreserve;
    }

    @Nonnull
    public List<String> getReadAreaInclude() {
        if (this.readAreaInclude == null) {
            return List.of();
        }
        return this.readAreaInclude;
    }

    @Nonnull
    public List<String> getReadAreaExclude() {
        if (this.readAreaExclude == null) {
            return List.of();
        }
        return this.readAreaExclude;
    }

    @Nonnull
    public List<String> getExecute() {
        if (this.execute == null) {
            return List.of();
        }
        if (execute.size() >= 1) {
            String arg = execute.get(0).toUpperCase();
            switch (arg) {
            case "PRIOR":
            case "EXCLUDING":
                break;
            default:
                throw new ParameterException("specify PRIOR or EXCLUDING for the first parameter of --execute");
            }
        }
        if (execute.size() >= 2) {
            String arg = execute.get(1).toUpperCase();
            switch (arg) {
            case "DEFERRABLE":
            case "IMMEDIATE":
                break;
            default:
                throw new ParameterException("specify DEFERRABLE or IMMEDIATE for the second parameter of --execute");
            }
        }
        return this.execute;
    }

    @Nonnull // has default value
    public String getLabel() {
        return this.label;
    }

    @Nonnull
    public Map<String, String> getWith() {
        return this.withMap;
    }

    @Nonnull
    public List<Supplier<Credential>> getCredentialList() {
        var list = new ArrayList<Supplier<Credential>>();
        if (this.user != null) {
            list.add(() -> {
                String password = ConfigUtil.readPassword();
                return new UsernamePasswordCredential(user, password);
            });
        }
        if (this.authToken != null) {
            list.add(() -> new RememberMeCredential(authToken));
        }
        if (this.credentials != null) {
            list.add(() -> {
                // var path = Paths.get(credentials);
                // TODO return FileCredential.load(path);
                throw new UnsupportedOperationException("not yet implemented --credentials");
            });
        }
        if (this.noAuth != null && this.noAuth) {
            list.add(() -> NullCredential.INSTANCE);
        }
        return list;
    }
}
