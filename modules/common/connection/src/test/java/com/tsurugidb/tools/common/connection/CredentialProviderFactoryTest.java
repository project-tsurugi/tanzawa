/*
 * Copyright 2023-2025 Project Tsurugi.
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
package com.tsurugidb.tools.common.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.tsurugidb.tsubakuro.channel.common.connection.FileCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.RememberMeCredential;
import com.tsurugidb.tsubakuro.channel.common.connection.UsernamePasswordCredential;

class CredentialProviderFactoryTest {

    private Path temporaryDir;

    Path getTemporaryDir() throws IOException {
        if (temporaryDir == null) {
            temporaryDir = Files.createTempDirectory(CredentialProviderFactoryTest.class.getSimpleName());
        }
        return temporaryDir;
    }

    @AfterEach
    void teardown() throws IOException {
        if (temporaryDir != null) {
            TestUtil.delete(temporaryDir);
        }
    }

    @Test
    void getNullCredentialProvider() throws Exception {
        var ps = new CredentialProviderFactory();

        var p = ps.getNullCredentialProvider();
        var c = p.get().orElseThrow();
        assertEquals(NullCredential.INSTANCE, c);
    }

    @Test
    void getPromptCredentialProvider() throws Exception {
        var ps = new CredentialProviderFactory();
        var prompt = mock(CredentialPrompt.class);
        doReturn(Optional.of("user")).when(prompt).getUsername();
        doReturn(Optional.of("pass")).when(prompt).getPassword();
        var p = ps.getPromptCredentialProvider(prompt, null);
        var c = p.get()
                .map(it -> (UsernamePasswordCredential) it)
                .orElseThrow();
        assertEquals("user", c.getName());
        assertEquals("pass", c.getPassword().orElse(null));
    }

    @Test
    void getPromptCredentialProvider_username() throws Exception {
        var ps = new CredentialProviderFactory();
        var prompt = mock(CredentialPrompt.class);
        doReturn(Optional.of("user")).when(prompt).getUsername();
        doReturn(Optional.of("pass")).when(prompt).getPassword();
        var p = ps.getPromptCredentialProvider(prompt, "USER");
        var c = p.get()
                .map(it -> (UsernamePasswordCredential) it)
                .orElseThrow();
        assertEquals("USER", c.getName());
        assertEquals("pass", c.getPassword().orElse(null));
    }

    @Test
    void getPromptCredentialProvider_empty_username() throws Exception {
        var ps = new CredentialProviderFactory();
        var prompt = mock(CredentialPrompt.class);
        assertThrows(IllegalArgumentException.class,
                () -> ps.getPromptCredentialProvider(prompt, ""));
    }

    @Test
    void getRememberMeCredentialProvider() throws Exception {
        var token = "remember-me-token";
        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.of("token")).when(ps).getDefaultAuthenticationToken();

        var p = ps.getRememberMeCredentialProvider(token);
        var c = p.get()
                .map(it -> (RememberMeCredential) it)
                .map(RememberMeCredential::getToken)
                .orElse(null);
        assertEquals(token, c);
    }

    @Test
    void getRememberMeCredentialProvider_empty_token() throws Exception {
        var ps = new CredentialProviderFactory();
        assertThrows(IllegalArgumentException.class, () -> ps.getRememberMeCredentialProvider(""));
    }

    @Test
    void getDefaultRememberMeCredentialProvider() throws Exception {
        var token = "remember-me-token";
        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.of(token)).when(ps).getDefaultAuthenticationToken();
        var p = ps.getDefaultRememberMeCredentialProvider();
        assertTrue(p.isPresent());

        var c = p.get().get()
                .map(it -> (RememberMeCredential) it)
                .map(RememberMeCredential::getToken)
                .orElse(null);

        assertEquals(token, c);
    }

    @Test
    void getDefaultRememberMeCredentialProvider_no_token() throws Exception {
        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.empty()).when(ps).getDefaultAuthenticationToken();
        var p = ps.getDefaultRememberMeCredentialProvider();
        assertTrue(p.isEmpty());
    }

    @Test
    void getFileCredentialProvider() throws Exception {
        var file = getTemporaryDir().resolve("creds.key");
        Files.writeString(file, "encrypted", StandardCharsets.UTF_8);

        var ps = new CredentialProviderFactory();
        var p = ps.getFileCredentialProvider(file);
        var c = p.get()
                .map(it -> (FileCredential) it)
                .map(FileCredential::getEncrypted)
                .orElse(null);

        assertEquals("encrypted", c);
    }

    @Test
    void getFileCredentialProvider_no_file() throws Exception {
        var file = getTemporaryDir().resolve("creds.key");
        // file does not exist

        var ps = new CredentialProviderFactory();
        var e = assertThrows(ConnectionException.class, () -> ps.getFileCredentialProvider(file));
        assertEquals(ConnectionDiagnosticCode.CREDENTIAL_ERROR, e.getDiagnosticCode());
    }

    @Test
    void getFileCredentialProvider_invalid_file() throws Exception {
        var file = getTemporaryDir().resolve("creds.key");
        Files.createFile(file); // empty file

        var ps = new CredentialProviderFactory();
        var e = assertThrows(ConnectionException.class, () -> ps.getFileCredentialProvider(file));
        assertEquals(ConnectionDiagnosticCode.CREDENTIAL_ERROR, e.getDiagnosticCode());
    }

    @Test
    void getDefaultFileCredentialProvider() throws Exception {
        var home = getTemporaryDir();
        var file = home.resolve(CredentialProviderFactory.DEFAULT_FILE_CREDENTIAL_PATH);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "encrypted", StandardCharsets.UTF_8);

        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.of(home)).when(ps).getUserHomePath();
        var p = ps.getDefaultFileCredentialProvider();
        assertTrue(p.isPresent());

        var c = p.get().get()
                .map(it -> (FileCredential) it)
                .map(FileCredential::getEncrypted)
                .orElse(null);

        assertEquals("encrypted", c);
    }

    @Test
    void getDefaultFileCredentialProvider_no_file() throws Exception {
        var home = getTemporaryDir();
        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.of(home)).when(ps).getUserHomePath();
        var p = ps.getDefaultFileCredentialProvider();
        assertTrue(p.isEmpty());
    }

    @Test
    void getDefaultFileCredentialProvider_no_home() throws Exception {
        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.empty()).when(ps).getUserHomePath();
        var p = ps.getDefaultFileCredentialProvider();
        assertTrue(p.isEmpty());
    }

    @Test
    void getDefaultFileCredentialProvider_invalid_file() throws Exception {
        var home = getTemporaryDir();
        var file = home.resolve(CredentialProviderFactory.DEFAULT_FILE_CREDENTIAL_PATH);
        Files.createDirectories(file.getParent());
        Files.createFile(file); // empty file

        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.of(home)).when(ps).getUserHomePath();
        assertThrows(IOException.class, () -> ps.getDefaultFileCredentialProvider());
    }

    @Test
    void getDefaultCredentialProviders() throws Exception {
        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.empty()).when(ps).getDefaultRememberMeCredentialProvider();
        doReturn(Optional.empty()).when(ps).getDefaultFileCredentialProvider();

        var list = ps.getDefaultCredentialProviders(null);
        assertEquals(1, list.size());
        assertEquals(CredentialProviderFactory.CREDENTIAL_TYPE_NULL, list.get(0).getType());
    }

    @Test
    void getDefaultCredentialProviders_token() throws Exception {
        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.of(new BasicCredentialProvider("x", NullCredential.INSTANCE)))
                .when(ps).getDefaultRememberMeCredentialProvider();
        doReturn(Optional.empty()).when(ps).getDefaultFileCredentialProvider();

        var list = ps.getDefaultCredentialProviders(null);
        assertEquals(2, list.size());
        assertEquals("x", list.get(0).getType());
        assertEquals(CredentialProviderFactory.CREDENTIAL_TYPE_NULL, list.get(1).getType());
    }

    @Test
    void getDefaultCredentialProviders_file() throws Exception {
        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.empty()).when(ps).getDefaultRememberMeCredentialProvider();
        doReturn(Optional.of(new BasicCredentialProvider("x", NullCredential.INSTANCE)))
                .when(ps).getDefaultFileCredentialProvider();

        var list = ps.getDefaultCredentialProviders(null);
        assertEquals(2, list.size());
        assertEquals("x", list.get(0).getType());
        assertEquals(CredentialProviderFactory.CREDENTIAL_TYPE_NULL, list.get(1).getType());
    }

    @Test
    void getDefaultCredentialProviders_file_io_error() throws Exception {
        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.empty()).when(ps).getDefaultRememberMeCredentialProvider();
        doThrow(new IOException()).when(ps).getDefaultFileCredentialProvider();

        var list = ps.getDefaultCredentialProviders(null);
        assertEquals(1, list.size());
        assertEquals(CredentialProviderFactory.CREDENTIAL_TYPE_NULL, list.get(0).getType());
    }

    @Test
    void getDefaultCredentialProviders_prompt() throws Exception {
        var ps = spy(new CredentialProviderFactory());
        doReturn(Optional.empty()).when(ps).getDefaultRememberMeCredentialProvider();
        doReturn(Optional.empty()).when(ps).getDefaultFileCredentialProvider();
        var prompt = mock(CredentialPrompt.class);
        var list = ps.getDefaultCredentialProviders(prompt);
        assertEquals(2, list.size());
        assertEquals(CredentialProviderFactory.CREDENTIAL_TYPE_NULL, list.get(0).getType());
        assertEquals(CredentialProviderFactory.CREDENTIAL_TYPE_USERNAME_PASSWORD, list.get(1).getType());
    }
}
