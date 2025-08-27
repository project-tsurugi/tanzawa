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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.InterruptedIOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class ReaderCredentialPromptTest {

    @Test
    void getUsername_simple() throws Exception {
        var writer = new StringWriter();
        var prompt = new ReaderCredentialPrompt(new StringReader("user"), writer);
        assertEquals(Optional.of("user"), prompt.getUsername());
        assertEquals(Optional.empty(), prompt.getUsername());
        assertFalse(writer.toString().isEmpty());
    }

    @Test
    void getUsername_trim() throws Exception {
        var prompt = new ReaderCredentialPrompt(new StringReader("  user  \n"));
        assertEquals(Optional.of("user"), prompt.getUsername());
        assertEquals(Optional.empty(), prompt.getUsername());
    }

    @Test
    void getUsername_empty() throws Exception {
        var prompt = new ReaderCredentialPrompt(new StringReader("\n"));
        assertEquals(Optional.empty(), prompt.getUsername());
    }

    @Test
    void getUsername_interrupted() throws Exception {
        var reader = spy(Reader.class);
        doThrow(new InterruptedIOException()).when(reader).read(any(), anyInt(), anyInt());
        var prompt = new ReaderCredentialPrompt(reader);
        assertThrows(InterruptedException.class, () -> prompt.getUsername());
    }

    @Test
    void getPassword_simple() throws Exception {
        var writer = new StringWriter();
        var prompt = new ReaderCredentialPrompt(new StringReader("password"), writer);
        assertEquals(Optional.of("password"), prompt.getPassword());
        assertEquals(Optional.empty(), prompt.getPassword());
        assertFalse(writer.toString().isEmpty());
    }

    @Test
    void getPassword_empty() throws Exception {
        var prompt = new ReaderCredentialPrompt(new StringReader("\n"));
        assertEquals(Optional.of(""), prompt.getPassword()); // password can be empty
        assertEquals(Optional.empty(), prompt.getPassword());
    }

    @Test
    void getPassword_interrupted() throws Exception {
        var reader = spy(Reader.class);
        doThrow(new InterruptedIOException()).when(reader).read(any(), anyInt(), anyInt());
        var prompt = new ReaderCredentialPrompt(reader);
        assertThrows(InterruptedException.class, () -> prompt.getPassword());
    }
}
