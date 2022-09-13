package com.tsurugidb.console.cli.argument;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

@Parameters
public class CommonArgument {

    @Parameter(names = { "--connection", "-c" }, arity = 1, description = "connection uri (e.g. tcp://localhost:12345)", required = true)
    private String connectionUri;

    @Parameter(names = { "--property", "-P" }, arity = 1, description = "SQL setting. <key>=<value>")
    private List<String> propertyList;

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

    @Parameter(names = { "--write-preserve", "-w" }, arity = 1, description = "write preserve. <table>[,<table>[,...]]")
    private String writePreserve;

    @Parameter(names = { "--user", "-u" }, arity = 1, description = "<user name>")
    private String user;

    @Parameter(names = { "--auth-token" }, arity = 1, description = "<token>")
    private String authToken;

    @Parameter(names = { "--credentials" }, arity = 1, description = "</path/to/credentials.json>")
    private String credentials;

    @Parameter(names = { "--no-auth" }, arity = 0, description = "no auth")
    private Boolean noAuth;

    @Nonnull
    public String getConnectionUri() {
        return this.connectionUri;
    }

    @Nonnull
    public List<String> getPropertyList() {
        if (this.propertyList == null) {
            return List.of();
        }
        return this.propertyList;
    }

    @Nonnull
    public TransactionEnum getTransaction() {
        return this.transaction;
    }

    @Nonnull
    public List<String> getWritePreserve() {
        if (this.writePreserve == null) {
            return List.of();
        }

        String[] ss = writePreserve.split(",");
        var list = new ArrayList<String>(ss.length);
        for (String s : ss) {
            list.add(s.trim());
        }
        return list;
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
            // TODO json file credential
            list.add(() -> {
                throw new UnsupportedOperationException("not yet implemented --credentials");
            });
        }
        if (this.noAuth != null && this.noAuth) {
            list.add(() -> NullCredential.INSTANCE);
        }
        return list;
    }
}
