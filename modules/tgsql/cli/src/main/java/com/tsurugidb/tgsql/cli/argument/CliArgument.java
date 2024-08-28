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
package com.tsurugidb.tgsql.cli.argument;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.CommaParameterSplitter;
import com.tsurugidb.sql.proto.SqlRequest.CommitStatus;
import com.tsurugidb.sql.proto.SqlRequest.TransactionType;
import com.tsurugidb.tgsql.cli.repl.ReplDefaultCredentialSessionConnector;
import com.tsurugidb.tgsql.core.TgsqlConstants;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.FileCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

/**
 * Argument for Tsurugi SQL console cli.
 */
@Parameters
public class CliArgument {

    @Parameter(names = { "--help", "-h" }, arity = 0, description = "print this message", help = true)
    private Boolean help;

    @Parameter(names = { "--version" }, arity = 0, description = "print version", help = true)
    private Boolean version;

    @Parameter(names = { "--console" }, arity = 0, description = "SQL console mode")
    private Boolean console;

    @Parameter(names = { "--script" }, arity = 0, description = "execute SQL script file mode")
    private Boolean script;

    @Parameter(names = { "--exec" }, arity = 0, description = "execute a SQL statement mode")
    private Boolean exec;

    @Parameter(names = { "--explain" }, arity = 0, description = "print explain mode", hidden = true)
    private Boolean explain;

    // connection

    @Parameter(names = { "--connection", "-c" }, arity = 1, description = "connection uri (e.g. tcp://localhost:12345)")
    private String connectionUri;

    @Parameter(names = { "--connection-label" }, arity = 1, description = "Tsurugi connection session label")
    private String connectionLabel;

    // credential

    @Parameter(names = { "--user", "-u" }, arity = 1, description = "<user name>")
    private String user;

    @Parameter(names = { "--auth-token" }, arity = 1, description = "<token>")
    private String authToken;

    @Parameter(names = { "--credentials" }, arity = 1, description = "</path/to/credentials.json>")
    private String credentials;

    @Parameter(names = { "--no-auth" }, arity = 0, description = "no auth")
    private Boolean noAuth;

    // transaction

    /**
     * value for --transaction.
     */
    public enum TransactionEnum {
        // OCC
        /** short. */
        SHORT("short", TransactionType.SHORT),
        /** OCC. */
        OCC("OCC", TransactionType.SHORT),
        // LTX
        /** long. */
        LONG("long", TransactionType.LONG),
        /** LTX. */
        LTX("LTX", TransactionType.LONG),
        // RTX
        /** read. */
        READ("read", TransactionType.READ_ONLY),
        /** readonly. */
        READONLY("readonly", TransactionType.READ_ONLY),
        /** ro. */
        RO("RO", TransactionType.READ_ONLY),
        /** RTX. */
        RTX("RTX", TransactionType.READ_ONLY),
        // manual
        /** manual. */
        MANUAL("manual", null);

        private final String value;
        private final TransactionType type;

        TransactionEnum(String value, TransactionType type) {
            this.value = value;
            this.type = type;
        }

        /**
         * get transaction type.
         *
         * @return transaction type
         */
        public TransactionType toTransactionType() {
            return this.type;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    @Parameter(names = { "--transaction", "-t" }, arity = 1, description = "transaction type")
    private TransactionEnum transaction = TransactionEnum.OCC;

    @Parameter(names = { "--include-ddl" }, arity = 0, description = "declares DDL execution")
    private Boolean includeDdl;

    @Parameter(names = { "--write-preserve", "-w" }, arity = 1, description = "write preserve. <table>[,<table>[,...]]", splitter = CommaParameterSplitter.class)
    private List<String> writePreserve;

    @Parameter(names = { "--read-area-include" }, arity = 1, description = "read area include. <table>[,<table>[,...]]", splitter = CommaParameterSplitter.class)
    private List<String> readAreaInclude;

    @Parameter(names = { "--read-area-exclude" }, arity = 1, description = "read area exclude. <table>[,<table>[,...]]", splitter = CommaParameterSplitter.class)
    private List<String> readAreaExclude;

    @Parameter(names = { "--execute" }, variableArity = true, description = "transaction execute. (PRIOR|EXCLUDING) (DEFERRABLE|IMMEDIATE)?")
    private List<String> execute;

    @Parameter(names = { "--label", "-l" }, arity = 1, description = "transaction label")
    private String label = TgsqlConstants.IMPLICIT_TRANSACTION_LABEL;

    @DynamicParameter(names = { "--with" }, description = "transaction setting. <key>=<value>")
    private Map<String, String> withMap = new LinkedHashMap<>();

    // commit

    @Parameter(names = { "--auto-commit" }, arity = 0, description = "commit every statement")
    private Boolean autoCommit;

    @Parameter(names = { "--no-auto-commit" }, arity = 0, description = "commit only if you explicitly specify a COMMIT statement")
    private Boolean noAutoCommit;

    @Parameter(names = { "--commit" }, arity = 0, description = "commit on success, rollback on failure")
    private Boolean commit;

    @Parameter(names = { "--no-commit" }, arity = 0, description = "always rollback")
    private Boolean noCommit;

    /**
     * commit option.
     */
    public enum CommitOption {
        /**
         * rely on the database settings.
         */
        DEFAULT(CommitStatus.COMMIT_STATUS_UNSPECIFIED),

        /**
         * commit operation has accepted (the transaction will never abort except system errors).
         */
        ACCEPTED(CommitStatus.ACCEPTED),

        /**
         * commit data has been visible for others.
         */
        AVAILABLE(CommitStatus.AVAILABLE),

        /**
         * commit data has been saved on the local disk.
         */
        STORED(CommitStatus.STORED),

        /**
         * commit data has been propagated to the all suitable nodes.
         */
        PROPAGATED(CommitStatus.PROPAGATED);

        private final CommitStatus commitStatus;

        CommitOption(CommitStatus commitStatus) {
            this.commitStatus = commitStatus;
        }

        public CommitStatus toCommitStatus() {
            return this.commitStatus;
        }
    }

    @Parameter(names = { "--commit-option" }, arity = 1, description = "commit option")
    private CommitOption commitOption;

    // script

    @Parameter(names = { "--encoding", "-e" }, arity = 1, description = "charset encoding")
    private String encoding = StandardCharsets.UTF_8.name();

    // explain (hidden)

    @Parameter(names = { "--input", "-i" }, arity = 1, description = "explain json file", hidden = true)
    private String inputFile;

    @Parameter(names = { "--report", "-r" }, arity = 0, description = "report to stdout", hidden = true)
    private Boolean report;

    @Parameter(names = { "--output", "-o" }, arity = 1, description = "output file (dot)", hidden = true)
    private String outputFile = null;

    @Parameter(names = { "--verbose", "-v" }, arity = 0, description = "verbose", hidden = true)
    private Boolean verbose;

    // other

    @DynamicParameter(names = { "--property", "-P" }, description = "SQL setting. <key>=<value>")
    private Map<String, String> propertyMap = new LinkedHashMap<>();

    @DynamicParameter(names = { "-D" }, description = "client variable. <key>=<value>")
    private Map<String, String> clientVariableMap = new LinkedHashMap<>();

    @Parameter(names = { "--client-variable" }, arity = 1, description = "client variable file. </path/to/client-variable.properties>")
    private String clientVariableFile;

    @Parameter(description = "</path/to/script.sql(--script) or statement(--exec)>")
    private List<String> otherList;

    //

    /**
     * get --help.
     *
     * @return help
     */
    public boolean isHelp() {
        return (this.help != null) && this.help;
    }

    /**
     * get --version.
     *
     * @return version
     */
    public boolean isVersion() {
        return (this.version != null) && this.version;
    }

    /**
     * get cli mode.
     *
     * @return cli mode
     */
    public @Nonnull CliMode getCliMode() {
        var list = new ArrayList<CliMode>();
        if (this.console != null && this.console) {
            list.add(CliMode.CONSOLE);
        }
        if (this.script != null && this.script) {
            list.add(CliMode.SCRIPT);
        }
        if (this.exec != null && this.exec) {
            list.add(CliMode.EXEC);
        }
        if (this.explain != null && this.explain) {
            list.add(CliMode.EXPLAIN);
        }

        switch (list.size()) {
        case 0:
            return CliMode.CONSOLE;
        case 1:
            return list.get(0);
        default:
            throw new ParameterException("specify only one of [--console, --script, --exec]");
        }
    }

    // connection

    /**
     * get --connection-uri.
     *
     * @return connection uri
     */
    public @Nonnull String getConnectionUri() {
        return this.connectionUri; // required = true
    }

    /**
     * get --connection-label.
     *
     * @return connection label
     */
    public @Nullable String getConnectionLabel() {
        return this.connectionLabel;
    }

    // credential

    /**
     * get credentials.
     *
     * @return credential list
     */
    public @Nonnull List<Supplier<Credential>> getCredentialList() {
        var list = new ArrayList<Supplier<Credential>>();
        if (this.user != null) {
            list.add(() -> {
                String password = readPassword();
                return new UsernamePasswordCredential(user, password);
            });
        }
        if (this.authToken != null) {
            list.add(() -> new RememberMeCredential(authToken));
        }
        if (this.credentials != null) {
            list.add(() -> {
                var path = Path.of(credentials);
                try {
                    return FileCredential.load(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e.getMessage(), e);
                }
            });
        }
        if (this.noAuth != null && this.noAuth) {
            list.add(() -> NullCredential.INSTANCE);
        }
        return list;
    }

    protected String readPassword() {
        return ReplDefaultCredentialSessionConnector.readReplPassword();
    }

    // transaction

    /**
     * get --transaction.
     *
     * @return transaction
     */
    public @Nonnull TransactionEnum getTransaction() {
        return this.transaction; // has default value
    }

    /**
     * get --include-ddl.
     *
     * @return include ddl
     */
    public boolean isIncludeDdl() {
        return (this.includeDdl != null) && this.includeDdl;
    }

    /**
     * get --write-preserve.
     *
     * @return write preserve
     */
    public @Nonnull List<String> getWritePreserve() {
        return normalizeList(this.writePreserve);
    }

    /**
     * get --read-area-include.
     *
     * @return read area include
     */
    public @Nonnull List<String> getReadAreaInclude() {
        return normalizeList(this.readAreaInclude);
    }

    /**
     * get --read-area-exclude.
     *
     * @return read area exclude
     */
    public @Nonnull List<String> getReadAreaExclude() {
        return normalizeList(this.readAreaExclude);
    }

    private List<String> normalizeList(List<String> list) {
        if (list == null) {
            return List.of();
        }
        return list.stream().map(String::trim).collect(Collectors.toList());
    }

    /**
     * check unknown parameter.
     *
     * @throws ParameterException invalid parameter
     */
    public void checkUnknownParameter() throws ParameterException {
        if (this.otherList == null || otherList.isEmpty()) {
            return; // OK
        }
        throw new ParameterException(MessageFormat.format("invalid parameter {0}", otherList));
    }

    /**
     * --execute.
     */
    public static class CliExecute {
        boolean prior = false;
        boolean deferrable = true;

        /**
         * get piror.
         *
         * @return {@code true} if prior
         */
        public boolean isPrior() {
            return this.prior;
        }

        /**
         * get excluding.
         *
         * @return {@code true} if excluding
         */
        public boolean isExcluding() {
            return !this.prior;
        }

        /**
         * get deferrable.
         *
         * @return {@code true} if deferrable
         */
        public boolean isDeferrable() {
            return this.deferrable;
        }

        /**
         * get immediate.
         *
         * @return {@code true} if immediate
         */
        public boolean isImmediate() {
            return !this.deferrable;
        }
    }

    /**
     * get --execute.
     *
     * @return execute
     */
    public @Nullable CliExecute getExecute() {
        if (this.execute == null) {
            return null;
        }

        var result = new CliExecute();
        if (execute.size() >= 1) {
            String arg = execute.get(0).toUpperCase();
            switch (arg) {
            case "PRIOR":
                result.prior = true;
                break;
            case "EXCLUDING":
                result.prior = false;
                break;
            default:
                throw new ParameterException("specify PRIOR or EXCLUDING for the first parameter of --execute");
            }
        }
        if (execute.size() >= 2) {
            String arg = execute.get(1).toUpperCase();
            switch (arg) {
            case "DEFERRABLE":
                result.deferrable = true;
                break;
            case "IMMEDIATE":
                result.deferrable = false;
                break;
            default:
                throw new ParameterException("specify DEFERRABLE or IMMEDIATE for the second parameter of --execute");
            }
        }
        return result;
    }

    /**
     * get --label.
     *
     * @return label
     */
    public @Nonnull String getLabel() {
        return this.label; // has default value
    }

    /**
     * get --with.
     *
     * @return with
     */
    public @Nonnull Map<String, String> getWith() {
        return this.withMap;
    }

    // commit

    /**
     * get --auto-commit.
     *
     * @return auto commit
     */
    public boolean getAutoCommit() {
        return (this.autoCommit != null) && this.autoCommit;
    }

    /**
     * get --no-auto-commit.
     *
     * @return no auto commit
     */
    public boolean getNoAutoCommit() {
        return (this.noAutoCommit != null) && this.noAutoCommit;
    }

    /**
     * get --commit.
     *
     * @return commit
     */
    public boolean getCommit() {
        return (this.commit != null) && this.commit;
    }

    /**
     * get --no-commit.
     *
     * @return no commit
     */
    public boolean getNoCommit() {
        return (this.noCommit != null) && this.noCommit;
    }

    /**
     * get --commit-option.
     *
     * @return commit option
     */
    public @Nullable CommitOption getCommitOption() {
        return this.commitOption;
    }

    // script

    /**
     * get --encoding.
     *
     * @return encoding
     */
    public @Nonnull String getEncoding() {
        return this.encoding;
    }

    /**
     * get script.
     *
     * @return script file path
     */
    public @Nonnull String getScript() {
        if (this.otherList == null || otherList.isEmpty()) {
            throw new ParameterException("specify /path/to/script.sql");
        }
        if (otherList.size() > 1) {
            throw new ParameterException(MessageFormat.format("contains invalid parameter {0}", otherList));
        }
        return otherList.get(0);
    }

    // exec

    /**
     * get SQL statement.
     *
     * @return statement
     */
    public @Nonnull String getStatement() {
        if (this.otherList == null || otherList.isEmpty()) {
            throw new ParameterException("specify SQL statement");
        }
        return String.join(" ", otherList);
    }

    // explain

    /**
     * get --input.
     *
     * @return explain json file path
     */
    public @Nonnull String getInputFile() {
        return this.inputFile;
    }

    /**
     * get --report.
     *
     * @return {@code true} if report
     */
    public boolean isReport() {
        return (this.report != null) && this.report;
    }

    /**
     * get --output.
     *
     * @return output file path
     */
    public @Nullable String getOutputFile() {
        return this.outputFile;
    }

    /**
     * get --verbose.
     *
     * @return {@code true} if verbose
     */
    public boolean isVerbose() {
        return (this.verbose != null) && this.verbose;
    }

    // other

    /**
     * get --property.
     *
     * @return property
     */
    public @Nonnull Map<String, String> getProperty() {
        return this.propertyMap;
    }

    /**
     * get -D.
     *
     * @return client variable
     */
    public @Nonnull Map<String, String> getClientVariable() {
        return this.clientVariableMap;
    }

    /**
     * get --client-variable.
     *
     * @return client variable file path
     */
    public @Nullable String getClientVariableFile() {
        return this.clientVariableFile;
    }
}
