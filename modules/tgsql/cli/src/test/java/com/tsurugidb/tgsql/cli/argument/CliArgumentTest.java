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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.tsurugidb.tgsql.cli.argument.CliArgument.TransactionEnum;
import com.tsurugidb.tgsql.core.TgsqlConstants;
import com.tsurugidb.tsubakuro.channel.common.connection.FileCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

class CliArgumentTest {

    private static final Map<String, Field> FIELD_MAP = new LinkedHashMap<>();
    static {
        BiConsumer<String[], Field> setter = (names, field) -> {
            for (String optionName : names) {
                if (FIELD_MAP.put(optionName, field) != null) {
                    throw new AssertionError(optionName);
                }
            }
        };

        for (var field : CliArgument.class.getDeclaredFields()) {
            var parameter = field.getAnnotation(Parameter.class);
            if (parameter != null) {
                setter.accept(parameter.names(), field);
            }
            var dynamicParameter = field.getAnnotation(DynamicParameter.class);
            if (dynamicParameter != null) {
                setter.accept(dynamicParameter.names(), field);
            }
            if (field.getName().equals("otherList")) {
                setter.accept(new String[] { "otherList" }, field);
            }
        }
    }

    @Test
    void cliModeDefault() {
        var argument = new CliArgument();

        assertEquals(CliMode.CONSOLE, argument.getCliMode());
    }

    @Test
    void cliModeConsole() {
        var argument = new CliArgument();
        set(argument, "--console", true);

        assertEquals(CliMode.CONSOLE, argument.getCliMode());
    }

    @Test
    void cliModeScript() {
        var argument = new CliArgument();
        set(argument, "--script", true);

        assertEquals(CliMode.SCRIPT, argument.getCliMode());
    }

    @Test
    void cliModeExec() {
        var argument = new CliArgument();
        set(argument, "--exec", true);

        assertEquals(CliMode.EXEC, argument.getCliMode());
    }

    @Test
    void cliModeExplain() {
        var argument = new CliArgument();
        set(argument, "--explain", true);

        assertEquals(CliMode.EXPLAIN, argument.getCliMode());
    }

    @Test
    void cliModeError() {
        var argument = new CliArgument();
        set(argument, "--console", true);
        set(argument, "--script", true);

        var e = assertThrows(ParameterException.class, () -> argument.getCliMode());
        assertEquals("specify only one of [--console, --script, --exec]", e.getMessage());
    }

    // connection

    @Test
    void connection() {
        String endpoint = "tcp://localhost:12345";
        for (String optionName : List.of("--connection", "-c")) {
            var argument = new CliArgument();
            set(argument, optionName, endpoint);

            assertEquals(endpoint, argument.getConnectionUri());
        }
    }

    @Test
    void connectionLabel() {
        String label = "test-label";
        var argument = new CliArgument();
        set(argument, "--connection-label", label);

        assertEquals(label, argument.getConnectionLabel());
    }

    // commit

    @Test
    void autoCommit() {
        testBoolean("--auto-commit", CliArgument::getAutoCommit);
    }

    @Test
    void noAutoCommit() {
        testBoolean("--no-auto-commit", CliArgument::getNoAutoCommit);
    }

    @Test
    void commit() {
        testBoolean("--commit", CliArgument::getCommit);
    }

    @Test
    void noCommit() {
        testBoolean("--no-commit", CliArgument::getNoCommit);
    }

    // property

    @Test
    void property() {
        for (String optionName : List.of("--property", "-P")) {
            var argument = new CliArgument();
            setMap(argument, optionName, "foo", "123");

            assertEquals(Map.of("foo", "123"), argument.getProperty());
        }
    }

    @Test
    void clientVariable() {
        for (String optionName : List.of("-D")) {
            var argument = new CliArgument();
            setMap(argument, optionName, "foo", "123");

            assertEquals(Map.of("foo", "123"), argument.getClientVariable());
        }
    }

    // transaction

    @Test
    void transaction() {
        {
            var argument = new CliArgument();
            assertEquals(TransactionEnum.OCC, argument.getTransaction());
        }

        for (String optionName : List.of("--transaction", "-t")) {
            var argument = new CliArgument();
            set(argument, optionName, TransactionEnum.LTX);

            assertEquals(TransactionEnum.LTX, argument.getTransaction());
        }
    }

    @Test
    void includeDdl() {
        {
            var argument = new CliArgument();
            assertFalse(argument.isIncludeDdl());
        }
        {
            var argument = new CliArgument();
            set(argument, "--include-ddl", true);

            assertTrue(argument.isIncludeDdl());
        }
    }

    @Test
    void writePreserve() {
        {
            var argument = new CliArgument();
            assertEquals(List.of(), argument.getWritePreserve());
        }

        for (String optionName : List.of("--write-preserve", "-w")) {
            var argument = new CliArgument();
            setList(argument, optionName, "test");

            assertEquals(List.of("test"), argument.getWritePreserve());
        }
    }

    @Test
    void readAreaInclude() {
        {
            var argument = new CliArgument();
            assertEquals(List.of(), argument.getReadAreaInclude());
        }

        for (String optionName : List.of("--read-area-include")) {
            var argument = new CliArgument();
            setList(argument, optionName, "test");

            assertEquals(List.of("test"), argument.getReadAreaInclude());
        }
    }

    @Test
    void readAreaExclude() {
        {
            var argument = new CliArgument();
            assertEquals(List.of(), argument.getReadAreaExclude());
        }

        for (String optionName : List.of("--read-area-exclude")) {
            var argument = new CliArgument();
            setList(argument, optionName, "test");

            assertEquals(List.of("test"), argument.getReadAreaExclude());
        }
    }

    @Test
    void checkUnknownParameter() {
        {
            var argument = new CliArgument();
            argument.checkUnknownParameter();
        }
        {
            var argument = new CliArgument();
            setList(argument, "otherList", "--zzz", "hoge");
            var e = assertThrows(ParameterException.class, () -> argument.checkUnknownParameter());
            assertEquals("invalid parameter [--zzz, hoge]", e.getMessage());
        }
    }

    @Test
    void execute() {
        {
            var argument = new CliArgument();
            assertNull(argument.getExecute());
        }

        {
            var argument = new CliArgument();
            setList(argument, "--execute", "prior");

            var execute = argument.getExecute();
            assertTrue(execute.isPrior());
            assertTrue(execute.isDeferrable());
        }
        {
            var argument = new CliArgument();
            setList(argument, "--execute", "excluding");

            var execute = argument.getExecute();
            assertTrue(execute.isExcluding());
            assertTrue(execute.isDeferrable());
        }
        for (String s1 : List.of("prior", "excluding")) {
            for (String s2 : List.of("deferrable", "immediate")) {
                var argument = new CliArgument();
                setList(argument, "--execute", s1, s2);

                var execute = argument.getExecute();
                assertEquals(s1.equals("prior"), execute.isPrior());
                assertEquals(s1.equals("excluding"), execute.isExcluding());
                assertEquals(s2.equals("deferrable"), execute.isDeferrable());
                assertEquals(s2.equals("immediate"), execute.isImmediate());
            }
        }

        {
            var argument = new CliArgument();
            setList(argument, "--execute", "immediate");

            var e = assertThrows(ParameterException.class, () -> argument.getExecute());
            assertEquals("specify PRIOR or EXCLUDING for the first parameter of --execute", e.getMessage());
        }
        {
            var argument = new CliArgument();
            setList(argument, "--execute", "prior", "hoge");

            var e = assertThrows(ParameterException.class, () -> argument.getExecute());
            assertEquals("specify DEFERRABLE or IMMEDIATE for the second parameter of --execute", e.getMessage());
        }
    }

    @Test
    void label() {
        {
            var argument = new CliArgument();
            assertEquals(TgsqlConstants.IMPLICIT_TRANSACTION_LABEL, argument.getLabel());
        }

        for (String optionName : List.of("--label")) {
            var argument = new CliArgument();
            set(argument, optionName, "test");

            assertEquals("test", argument.getLabel());
        }
    }

    @Test
    void with() {
        for (String optionName : List.of("--with")) {
            var argument = new CliArgument();
            setMap(argument, optionName, "foo", "123");

            assertEquals(Map.of("foo", "123"), argument.getWith());
        }
    }

    // credential

    @Test
    void credentialUser() {
        for (String optionName : List.of("--user", "-u")) {
            var argument = new CliArgument() {
                @Override
                protected String readPassword() {
                    return "password1";
                }
            };
            set(argument, optionName, "user1");

            var list = argument.getCredentialList();
            assertEquals(1, list.size());
            var credential = list.get(0).get();
            assertInstanceOf(UsernamePasswordCredential.class, credential);
            var usernamePasswordCredential = (UsernamePasswordCredential) credential;
            assertEquals("user1", usernamePasswordCredential.getName());
            assertEquals(Optional.of("password1"), usernamePasswordCredential.getPassword());
        }
    }

    @Test
    void credentialAuthToken() {
        for (String optionName : List.of("--auth-token")) {
            var argument = new CliArgument();
            set(argument, optionName, "token1");

            var list = argument.getCredentialList();
            assertEquals(1, list.size());
            var credential = list.get(0).get();
            assertInstanceOf(RememberMeCredential.class, credential);
            var rememberMeCredential = (RememberMeCredential) credential;
            assertEquals("token1", rememberMeCredential.getToken());
        }
    }

    @Test
    void credentialFile() {
        for (String optionName : List.of("--credentials")) {
            var argument = new CliArgument();
            set(argument, optionName, "/tmp");

            var list = argument.getCredentialList();
            assertEquals(1, list.size());
            try {
                var credential = list.get(0).get();
                assertInstanceOf(FileCredential.class, credential);
            } catch (UncheckedIOException e) {
                // through
            }
        }
    }

    @Test
    void credentialNoAuth() {
        for (String optionName : List.of("--no-auth")) {
            var argument = new CliArgument();
            set(argument, optionName, true);

            var list = argument.getCredentialList();
            assertEquals(1, list.size());
            var credential = list.get(0).get();
            assertInstanceOf(NullCredential.class, credential);
        }
    }

    // script

    @Test
    void encoding() {
        for (String optionName : List.of("--encoding", "-e")) {
            var argument = new CliArgument();
            set(argument, optionName, "UTF-8");

            assertEquals("UTF-8", argument.getEncoding());
        }
    }

    @Test
    void script() {
        {
            var argument = new CliArgument();
            setList(argument, "otherList", "script.sql");

            assertEquals("script.sql", argument.getScript());
        }
        {
            var argument = new CliArgument();
            setList(argument, "otherList", "script.sql", "hoge");

            var e = assertThrows(ParameterException.class, () -> argument.getScript());
            assertEquals("contains invalid parameter [script.sql, hoge]", e.getMessage());
        }
    }

    @Test
    void statement() {
        var argument = new CliArgument();
        setList(argument, "otherList", "select", "*", "from", "test");

        assertEquals("select * from test", argument.getStatement());
    }

    // explain

    @Test
    void input() {
        for (String optionName : List.of("--input", "-i")) {
            var argument = new CliArgument();
            set(argument, optionName, "input.json");

            assertEquals("input.json", argument.getInputFile());
        }
    }

    @Test
    void report() {
        for (String optionName : List.of("--report", "-r")) {
            testBoolean(optionName, CliArgument::isReport);
        }
    }

    @Test
    void output() {
        for (String optionName : List.of("--output", "-o")) {
            var argument = new CliArgument();
            set(argument, optionName, "output.dot");

            assertEquals("output.dot", argument.getOutputFile());
        }
    }

    @Test
    void verbose() {
        for (String optionName : List.of("--verbose", "-v")) {
            testBoolean(optionName, CliArgument::isVerbose);
        }
    }

    //

    private static void testBoolean(String optionName, Predicate<CliArgument> getter) {
        {
            var argument = new CliArgument();
            assertFalse(getter.test(argument));
        }
        {
            var argument = new CliArgument();
            set(argument, optionName, true);
            assertTrue(getter.test(argument));
        }
        {
            var argument = new CliArgument();
            set(argument, optionName, false);
            assertFalse(getter.test(argument));
        }
    }

    private static void set(CliArgument argument, String optionName, Object value) {
        try {
            var field = getFeild(optionName);
            field.setAccessible(true);
            field.set(argument, value);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setMap(CliArgument argument, String optionName, String key, String value) {
        try {
            var field = getFeild(optionName);
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var map = (Map<String, String>) field.get(argument);
            map.put(key, value);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void setList(CliArgument argument, String optionName, String... values) {
        try {
            var field = getFeild(optionName);
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var list = (List<String>) field.get(argument);
            if (list == null) {
                list = new ArrayList<>();
                field.set(argument, list);
            }
            for (String value : values) {
                list.add(value);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Field getFeild(String optionName) {
        var field = FIELD_MAP.get(optionName);
        if (field == null) {
            throw new AssertionError(optionName);
        }
        return field;
    }
}
