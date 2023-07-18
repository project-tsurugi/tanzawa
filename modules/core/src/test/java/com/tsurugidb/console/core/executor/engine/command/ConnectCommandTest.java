package com.tsurugidb.console.core.executor.engine.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.credential.DefaultCredentialSessionConnector;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.console.core.parser.SqlParser;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

class ConnectCommandTest {

    private static final ScriptConfig config = new ScriptConfig();
    static {
        config.setDefaultCredentialSessionConnector(new DefaultCredentialSessionConnector() {

            @Override
            public String readUser() {
                return "input-user";
            }

            @Override
            public String readPassword() {
                return "input-password";
            }
        });
    }

    @Test
    void endpoint() throws Exception {
        var target = new ConnectCommand();

        var statement = parse("tcp://test:12345", "");
        var option = target.parseOption(config, statement);

        assertEquals("tcp://test:12345", option.endpoint);
        assertEquals(0, option.credentialList.size());
    }

    @ParameterizedTest
    @ValueSource(strings = { "ipc:test", "" })
    void user0(String endpoint) throws Exception {
        var target = new ConnectCommand();

        var statement = parse(endpoint, "user");
        var option = target.parseOption(config, statement);

        if (endpoint.isEmpty()) {
            assertNull(option.endpoint);
        } else {
            assertEquals("ipc:test", option.endpoint);
        }

        assertEquals(1, option.credentialList.size());
        var credential = (UsernamePasswordCredential) option.credentialList.get(0).get();
        assertEquals("input-user", credential.getName());
        assertEquals(Optional.of("input-password"), credential.getPassword());
    }

    @ParameterizedTest
    @ValueSource(strings = { "ipc:test", "" })
    void user1(String endpoint) throws Exception {
        var target = new ConnectCommand();

        var statement = parse(endpoint, "user abc");
        var option = target.parseOption(config, statement);

        if (endpoint.isEmpty()) {
            assertNull(option.endpoint);
        } else {
            assertEquals("ipc:test", option.endpoint);
        }

        assertEquals(1, option.credentialList.size());
        var credential = (UsernamePasswordCredential) option.credentialList.get(0).get();
        assertEquals("abc", credential.getName());
        assertEquals(Optional.of("input-password"), credential.getPassword());
    }

    @ParameterizedTest
    @ValueSource(strings = { "ipc:test", "" })
    void user2(String endpoint) throws Exception {
        var target = new ConnectCommand();

        var statement = parse(endpoint, "user abc def");
        var option = target.parseOption(config, statement);

        if (endpoint.isEmpty()) {
            assertNull(option.endpoint);
        } else {
            assertEquals("ipc:test", option.endpoint);
        }

        assertEquals(1, option.credentialList.size());
        var credential = (UsernamePasswordCredential) option.credentialList.get(0).get();
        assertEquals("abc", credential.getName());
        assertEquals(Optional.of("def"), credential.getPassword());
    }

    @ParameterizedTest
    @ValueSource(strings = { "ipc:test", "" })
    void token0(String endpoint) throws Exception {
        var target = new ConnectCommand();

        var statement = parse(endpoint, "auth-token");
        assertThrows(IllegalArgumentException.class, () -> {
            target.parseOption(config, statement);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "ipc:test", "" })
    void token1(String endpoint) throws Exception {
        var target = new ConnectCommand();

        var statement = parse(endpoint, "auth-token abcdefg");
        var option = target.parseOption(config, statement);

        if (endpoint.isEmpty()) {
            assertNull(option.endpoint);
        } else {
            assertEquals("ipc:test", option.endpoint);
        }

        assertEquals(1, option.credentialList.size());
        var credential = (RememberMeCredential) option.credentialList.get(0).get();
        assertEquals("abcdefg", credential.getToken());
    }

    @ParameterizedTest
    @ValueSource(strings = { "ipc:test", "" })
    void credentials0(String endpoint) throws Exception {
        var target = new ConnectCommand();

        var statement = parse(endpoint, "credentials");
        assertThrows(IllegalArgumentException.class, () -> {
            target.parseOption(config, statement);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "ipc:test", "" })
    void credentials1(String endpoint) throws Exception {
        var target = new ConnectCommand();

        var statement = parse(endpoint, "credentials /tmp/tsurugi-tanzawa-test/");
        var option = target.parseOption(config, statement);

        if (endpoint.isEmpty()) {
            assertNull(option.endpoint);
        } else {
            assertEquals("ipc:test", option.endpoint);
        }

        assertEquals(1, option.credentialList.size());
        var supplier = option.credentialList.get(0);
        assertThrows(UncheckedIOException.class, () -> {
            supplier.get();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = { "ipc:test", "" })
    void noAuth(String endpoint) throws Exception {
        var target = new ConnectCommand();

        var statement = parse(endpoint, "no-auth");
        var option = target.parseOption(config, statement);

        if (endpoint.isEmpty()) {
            assertNull(option.endpoint);
        } else {
            assertEquals("ipc:test", option.endpoint);
        }

        assertEquals(1, option.credentialList.size());
        var credential = option.credentialList.get(0).get();
        assertInstanceOf(NullCredential.class, credential);
    }

    private static SpecialStatement parse(String endpoint, String argText) throws IOException {
        String text = "\\connect " + endpoint + " " + argText;
        try (var parser = new SqlParser(new StringReader(text))) {
            return (SpecialStatement) parser.next();
        }
    }
}
