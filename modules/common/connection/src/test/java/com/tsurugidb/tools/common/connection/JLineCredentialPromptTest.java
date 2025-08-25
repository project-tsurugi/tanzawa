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

import java.io.IOError;
import java.io.IOException;
import java.util.Optional;

import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.junit.jupiter.api.Test;

class JLineCredentialPromptTest {

    @Test
    void getUsername() throws Exception {
        LineReader reader = mock(LineReader.class);
        doReturn("user1").when(reader).readLine(anyString());
        var prompt = new JLineCredentialPrompt(reader);

        var result = prompt.getUsername();
        assertEquals(Optional.of("user1"), result);
    }

    @Test
    void getUsername_empty() throws Exception {
        LineReader reader = mock(LineReader.class);
        doReturn("").when(reader).readLine(anyString());
        var prompt = new JLineCredentialPrompt(reader);

        var result = prompt.getUsername();
        assertEquals(Optional.empty(), result);
    }

    @Test
    void getUsername_interrupt() throws Exception {
        LineReader reader = mock(LineReader.class);
        doThrow(new UserInterruptException("")).when(reader).readLine(anyString());
        var prompt = new JLineCredentialPrompt(reader);

        var result = prompt.getUsername();
        assertEquals(Optional.empty(), result);
    }

    @Test
    void getUsername_ioerror() throws Exception {
        LineReader reader = mock(LineReader.class);
        doThrow(new IOError(new IOException())).when(reader).readLine(anyString());
        var prompt = new JLineCredentialPrompt(reader);

        assertThrows(IOException.class, prompt::getUsername);
    }

    @Test
    void getUsername_interrupted() throws Exception {
        LineReader reader = mock(LineReader.class);
        doThrow(new UserInterruptException("")).when(reader).readLine(anyString());
        var prompt = new JLineCredentialPrompt(reader);

        var result = prompt.getUsername();
        assertEquals(Optional.empty(), result);
    }

    @Test
    void getPassword() throws Exception {
        LineReader reader = mock(LineReader.class);
        doReturn("pass1").when(reader).readLine(anyString(), anyChar());
        var prompt = new JLineCredentialPrompt(reader);

        var result = prompt.getPassword();
        assertEquals(Optional.of("pass1"), result);
    }

    @Test
    void getPassword_empty() throws Exception {
        LineReader reader = mock(LineReader.class);
        doReturn("").when(reader).readLine(anyString(), anyChar());
        var prompt = new JLineCredentialPrompt(reader);

        var result = prompt.getPassword();
        assertEquals(Optional.of(""), result);
    }

    @Test
    void getPassword_ioerror() throws Exception {
        LineReader reader = mock(LineReader.class);
        doThrow(new IOError(new IOException())).when(reader).readLine(anyString(), anyChar());
        var prompt = new JLineCredentialPrompt(reader);

        assertThrows(IOException.class, prompt::getPassword);
    }

    @Test
    void getPassword_interrupted() throws Exception {
        LineReader reader = mock(LineReader.class);
        doThrow(new UserInterruptException("")).when(reader).readLine(anyString(), anyChar());
        var prompt = new JLineCredentialPrompt(reader);

        var result = prompt.getPassword();
        assertEquals(Optional.empty(), result);
    }
}
