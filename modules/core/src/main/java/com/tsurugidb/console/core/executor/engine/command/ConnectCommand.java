package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.FileCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Connect command for Tsurugi SQL console.
 */
public class ConnectCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectCommand.class);

    private static final String COMMAND_NAME = "connect"; //$NON-NLS-1$
    private static final String COMMAND = COMMAND_PREFIX + COMMAND_NAME;

    /**
     * Creates a new instance.
     */
    public ConnectCommand() {
        super(false, COMMAND_NAME);
    }

    private static class CandidateOption {
        private final String option;
        private final boolean end;

        CandidateOption(String option, boolean end) {
            this.option = option;
            this.end = end;
        }

        public String option() {
            return this.option;
        }

        public boolean end() {
            return this.end;
        }
    }

    private static final List<CandidateOption> CANDIDATE_OPTION_LIST = List.of(//
            new CandidateOption("user", false), //$NON-NLS-1$
            new CandidateOption("auth-token", false), //$NON-NLS-1$
            new CandidateOption("credentials", false), //$NON-NLS-1$
            new CandidateOption("no-auth", true), //$NON-NLS-1$
            new CandidateOption("default", true)); //$NON-NLS-1$

    @Override
    public List<CompleterCandidateWords> getDynamicCompleterCandidateList(ScriptConfig config, String[] inputWords) {
        switch (inputWords.length) {
        case 2:
            return CANDIDATE_OPTION_LIST.stream().map(s -> new CompleterCandidateWords(COMMAND, s.option(), s.end())).collect(Collectors.toList());
        case 3:
            return CANDIDATE_OPTION_LIST.stream().map(s -> new CompleterCandidateWords(COMMAND, inputWords[1], s.option(), s.end())).collect(Collectors.toList());
        default:
            return List.of();
        }
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        LOG.debug("starting connect"); //$NON-NLS-1$
        var config = engine.getConfig();
        var option = parseOption(config, statement);

        String endpoint = option.endpoint;
        if (endpoint != null) {
            config.setEndpoint(endpoint);
        } else {
            endpoint = config.getEndpoint();
            if (endpoint == null) {
                throw new IllegalArgumentException("connection-url not specified");
            }
        }

        var list = option.credentialList;
        switch (list.size()) {
        case 0:
            break;
        case 1:
            var credentialSupplier = list.get(0);
            config.setCredential(null);
            config.setCredentialSupplier(credentialSupplier);
            break;
        default:
            throw new IllegalArgumentException("specify only one of [user, auth-token, credentials, no-auth, default]");
        }

        engine.connect();

        var reporter = engine.getReporter();
        reporter.reportConnect(endpoint);

        return true;
    }

    static class ConnectOption {
        String endpoint;
        List<Supplier<Credential>> credentialList = new ArrayList<>();
    }

    ConnectOption parseOption(ScriptConfig config, SpecialStatement statement) {
        var option = new ConnectOption();
        int size = statement.getCommandOptions().size();
        for (int i = 0; i < size; i++) {
            String s = getOption(statement, i);
            switch (s) {
            case "user":
                String user, password; {
                String s1 = getOption(statement, i + 1);
                if (s1 == null) {
                    user = null;
                    password = null;
                } else {
                    user = s1;
                    i++;
                    String s2 = getOption(statement, i + 1);
                    if (s2 == null) {
                        password = null;
                    } else {
                        password = s2;
                        i++;
                    }
                }
            }
                if (user == null) {
                    option.credentialList.add(() -> {
                        var defaultCredentialSupplier = config.getDefaultCredentialSupplier();
                        String u = defaultCredentialSupplier.readUser();
                        String p = defaultCredentialSupplier.readPassword();
                        return new UsernamePasswordCredential(u, p);
                    });
                } else if (password == null) {
                    option.credentialList.add(() -> {
                        var defaultCredentialSupplier = config.getDefaultCredentialSupplier();
                        String p = defaultCredentialSupplier.readPassword();
                        return new UsernamePasswordCredential(user, p);
                    });
                } else {
                    option.credentialList.add(() -> new UsernamePasswordCredential(user, password));
                }
                break;
            case "auth-token":
            case "token":
                String token = getOption(statement, i + 1);
                if (token == null) {
                    throw new IllegalArgumentException("token not specified");
                } else {
                    i++;
                    option.credentialList.add(() -> new RememberMeCredential(token));
                }
                break;
            case "credentials":
                String file = getOption(statement, i + 1);
                if (file == null) {
                    throw new IllegalArgumentException("credential.json not specified");
                } else {
                    i++;
                    option.credentialList.add(() -> {
                        var path = Path.of(file);
                        try {
                            return FileCredential.load(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e.getMessage(), e);
                        }
                    });
                }
                break;
            case "no-auth":
                option.credentialList.add(() -> NullCredential.INSTANCE);
                break;
            case "default":
                option.credentialList.add(() -> {
                    var defaultCredentialSupplier = config.getDefaultCredentialSupplier();
                    return defaultCredentialSupplier.getDefaultCredential();
                });
                break;
            default:
                if (option.endpoint == null) {
                    option.endpoint = s;
                } else {
                    throw new IllegalArgumentException(MessageFormat.format("unknown option [{0}]", s));
                }
                break;
            }
        }
        return option;
    }
}
