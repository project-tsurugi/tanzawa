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
package com.tsurugidb.tools.tgdump.cli;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CommandArgumentSetTest {

    @Test
    void getProfileReader() {
        var args = new CommandArgumentSet();
        assertNotNull(args.getProfileReader());
    }

    @Test
    void getProfileBundleLoader() {
        var args = new CommandArgumentSet();
        assertNotNull(args.getProfileBundleLoader());
    }

    @Test
    void getTargetSelector() {
        var args = new CommandArgumentSet();
        assertNotNull(args.getTargetSelector());
    }

    @Test
    void getConnectionProvider() {
        var args = new CommandArgumentSet();
        assertNotNull(args.getConnectionProvider());
    }
}
