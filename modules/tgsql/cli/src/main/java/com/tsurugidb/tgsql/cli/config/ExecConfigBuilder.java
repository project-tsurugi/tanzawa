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

import javax.annotation.Nonnull;

import com.tsurugidb.tgsql.cli.argument.CliArgument;
import com.tsurugidb.tgsql.core.config.TgsqlCommitMode;

/**
 * ConfigBuilder for exec.
 */
public class ExecConfigBuilder extends ConfigBuilder {

    private String statement;

    /**
     * Creates a new instance.
     *
     * @param argument argument
     */
    public ExecConfigBuilder(CliArgument argument) {
        super(argument);
    }

    @Override
    protected void buildSub() {
        fillCommitMode(EnumSet.of(TgsqlCommitMode.COMMIT, TgsqlCommitMode.NO_COMMIT), //
                TgsqlCommitMode.COMMIT);

        fillStatement();
    }

    private void fillStatement() {
        this.statement = argument.getStatement();
        log.debug("config.statement=[{}]", statement);
    }

    /**
     * get SQL statement.
     *
     * @return statement
     */
    @Nonnull
    public String getStatement() {
        return this.statement;
    }
}
