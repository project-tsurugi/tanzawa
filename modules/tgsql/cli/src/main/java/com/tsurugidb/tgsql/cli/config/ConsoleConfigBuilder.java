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
package com.tsurugidb.tgsql.cli.config;

import java.util.EnumSet;

import com.tsurugidb.tgsql.cli.argument.CliArgument;
import com.tsurugidb.tgsql.core.config.TgsqlClientVariableMap;
import com.tsurugidb.tgsql.core.config.TgsqlCommitMode;
import com.tsurugidb.tgsql.core.config.TgsqlCvKey;

/**
 * ConfigBuilder for console.
 */
public class ConsoleConfigBuilder extends ConfigBuilder {

    /**
     * Creates a new instance.
     *
     * @param argument argument
     */
    public ConsoleConfigBuilder(CliArgument argument) {
        super(argument);
    }

    @Override
    protected void fillClientVariableDefault(TgsqlClientVariableMap clientVariableMap) {
        clientVariableMap.put(TgsqlCvKey.SELECT_MAX_LINES, 1000);
        clientVariableMap.put(TgsqlCvKey.SQL_TIMING, true);
    }

    @Override
    protected void buildSub() {
        fillCommitMode(EnumSet.of(TgsqlCommitMode.AUTO_COMMIT, TgsqlCommitMode.NO_AUTO_COMMIT), //
                TgsqlCommitMode.NO_AUTO_COMMIT);
    }
}
