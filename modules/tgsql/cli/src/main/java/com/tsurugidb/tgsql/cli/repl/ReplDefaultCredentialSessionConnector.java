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
package com.tsurugidb.tgsql.cli.repl;

import javax.annotation.Nonnull;

import com.tsurugidb.tgsql.cli.repl.jline.ReplJLineReader;
import com.tsurugidb.tgsql.core.credential.DefaultCredentialSessionConnector;

public class ReplDefaultCredentialSessionConnector extends DefaultCredentialSessionConnector {

    @Override
    public String readUser() {
        return readReplUser();
    }

    @Override
    public String readPassword() {
        return readReplPassword();
    }

    /**
     * get user from console.
     *
     * @return user
     */
    public static @Nonnull String readReplUser() {
        var lineReader = ReplJLineReader.createSimpleReader();
        return lineReader.readLine("user: ");
    }

    /**
     * get password from console.
     *
     * @return password
     */
    public static @Nonnull String readReplPassword() {
        var lineReader = ReplJLineReader.createSimpleReader();
        return lineReader.readLine("password: ", '*');
    }
}
