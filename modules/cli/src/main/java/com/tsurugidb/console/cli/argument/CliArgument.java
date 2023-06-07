package com.tsurugidb.console.cli.argument;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.CommaParameterSplitter;
import com.tsurugidb.console.cli.repl.ReplCredentialSupplier;
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

    // commit

    @Parameter(names = { "--auto-commit" }, arity = 0, description = "commit every statement")
    private Boolean autoCommit;

    @Parameter(names = { "--no-auto-commit" }, arity = 0, description = "commit only if you explicitly specify a COMMIT statement")
    private Boolean noAutoCommit;

    @Parameter(names = { "--commit" }, arity = 0, description = "commit on success, rollback on failure")
    private Boolean commit;

    @Parameter(names = { "--no-commit" }, arity = 0, description = "always rollback")
    private Boolean noCommit;

    // property

    @DynamicParameter(names = { "--property", "-P" }, description = "SQL setting. <key>=<value>")
    private Map<String, String> propertyMap = new LinkedHashMap<>();

    @DynamicParameter(names = { "-D" }, description = "client variable. <key>=<value>")
    private Map<String, String> clientVariableMap = new LinkedHashMap<>();

    @Parameter(names = { "--client-variable" }, arity = 1, description = "client variable file. </path/to/client-variable.properties>")
    private String clientVariableFile;

    // transaction

    /**
     * value for --transaction.
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

    @Parameter(names = { "--transaction", "-t" }, arity = 1, description = "transaction type")
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

    // credential

    @Parameter(names = { "--user", "-u" }, arity = 1, description = "<user name>")
    private String user;

    @Parameter(names = { "--auth-token" }, arity = 1, description = "<token>")
    private String authToken;

    @Parameter(names = { "--credentials" }, arity = 1, description = "</path/to/credentials.json>")
    private String credentials;

    @Parameter(names = { "--no-auth" }, arity = 0, description = "no auth")
    private Boolean noAuth;

    // script

    @Parameter(names = { "--encoding", "-e" }, arity = 1, description = "charset encoding")
    private String encoding = Charset.defaultCharset().name();

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

    @Parameter(description = "</path/to/script.sql(--script) or statement(--exec)>")
    private List<String> otherList;

    //

    /**
     * get --help.
     *
     * @return help
     */
    public boolean isHelp() {
        return this.help != null && this.help;
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

    // property

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
     * get --write-preserve.
     *
     * @return write preserve
     */
    public @Nonnull List<String> getWritePreserve() {
        if (this.writePreserve == null) {
            return List.of();
        }
        return this.writePreserve;
    }

    /**
     * get --read-area-include.
     *
     * @return read area include
     */
    public @Nonnull List<String> getReadAreaInclude() {
        if (this.readAreaInclude == null) {
            return List.of();
        }
        return this.readAreaInclude;
    }

    /**
     * get --read-area-exclude.
     *
     * @return read area exclude
     */
    public @Nonnull List<String> getReadAreaExclude() {
        if (this.readAreaExclude == null) {
            return List.of();
        }
        return this.readAreaExclude;
    }

    /**
     * get --execute.
     *
     * @return execute
     */
    public @Nonnull List<String> getExecute() {
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
        return ReplCredentialSupplier.readReplPassword();
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
}
