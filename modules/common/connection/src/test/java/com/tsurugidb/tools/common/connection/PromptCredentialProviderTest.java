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
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;

class PromptCredentialProviderTest {

    @Test
    void simple() throws Exception {
        var prompt = mock(CredentialPrompt.class);
        doReturn(Optional.of("user")).when(prompt).getUsername();
        doReturn(Optional.of("pass")).when(prompt).getPassword();

        var provider = new PromptCredentialProvider("testing", prompt);
        System.out.println(provider);

        var credential = provider.get();
        assertTrue(credential.isPresent());
        assertEquals("user", credential.get().getName());
        assertEquals(Optional.of("pass"), credential.get().getPassword());
    }

    @Test
    void without_username() throws Exception {
        var prompt = mock(CredentialPrompt.class);
        doReturn(Optional.of("user")).when(prompt).getUsername();
        doReturn(Optional.of("pass")).when(prompt).getPassword();

        var provider = new PromptCredentialProvider("testing", prompt);
        System.out.println(provider);

        var credential = provider.get();
        assertTrue(credential.isPresent());
        assertEquals("user", credential.get().getName());
        assertEquals(Optional.of("pass"), credential.get().getPassword());
    }

    @Test
    void username_canceled() throws Exception {
        var prompt = mock(CredentialPrompt.class);
        doReturn(Optional.empty()).when(prompt).getUsername();
        doReturn(Optional.of("pass")).when(prompt).getPassword();

        var provider = new PromptCredentialProvider("testing", prompt);
        var credential = provider.get();
        assertTrue(credential.isEmpty());
    }

    @Test
    void password_canceled() throws Exception {
        var prompt = mock(CredentialPrompt.class);
        doReturn(Optional.of("user")).when(prompt).getUsername();
        doReturn(Optional.empty()).when(prompt).getPassword();

        var provider = new PromptCredentialProvider("testing", prompt);
        var credential = provider.get();
        assertTrue(credential.isEmpty());
    }

    @Test
    void ctor_empty_username() throws Exception {
        var prompt = mock(CredentialPrompt.class);

        assertThrows(IllegalArgumentException.class,
                () -> new PromptCredentialProvider("testing", prompt, ""));
    }

    @Test
    void raise_io_exception() throws Exception {
        var prompt = mock(CredentialPrompt.class);
        doThrow(new IOException()).when(prompt).getUsername();

        var provider = new PromptCredentialProvider("testing", prompt);
        var e = assertThrows(DiagnosticException.class, provider::get);
        assertEquals(ConnectionDiagnosticCode.CREDENTIAL_ERROR, e.getDiagnosticCode());
    }

    @Test
    void raise_interrupted_exception() throws Exception {
        var prompt = mock(CredentialPrompt.class);
        doThrow(new InterruptedException()).when(prompt).getUsername();

        var provider = new PromptCredentialProvider("testing", prompt);
        assertThrows(InterruptedException.class, provider::get);
    }
}
