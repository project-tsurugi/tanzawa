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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import javax.annotation.Nonnull;

import com.beust.jcommander.ParameterException;
import com.tsurugidb.tgsql.cli.argument.CliArgument;
import com.tsurugidb.tgsql.core.config.TgsqlCommitMode;

/**
 * ConfigBuilder for script.
 */
public class ScriptConfigBuilder extends ConfigBuilder {

    private Charset encoding;
    private Path script;

    /**
     * Creates a new instance.
     *
     * @param argument argument
     */
    public ScriptConfigBuilder(CliArgument argument) {
        super(argument);
    }

    @Override
    protected void buildSub() {
        fillCommitMode(EnumSet.of(TgsqlCommitMode.AUTO_COMMIT, TgsqlCommitMode.NO_AUTO_COMMIT, TgsqlCommitMode.COMMIT, TgsqlCommitMode.NO_COMMIT), //
                TgsqlCommitMode.COMMIT);

        fillEncoding();
        fillScript();
    }

    private void fillEncoding() {
        try {
            this.encoding = Charset.forName(argument.getEncoding());
        } catch (Exception e) {
            throw new RuntimeException("invalid encoding", e);
        }
        log.debug("config.encoding={}", encoding);
    }

    /**
     * get script file encoding.
     *
     * @return encoding
     */
    @Nonnull
    public Charset getEncoding() {
        return this.encoding;
    }

    private void fillScript() {
        try {
            this.script = Path.of(argument.getScript());
            if (!Files.exists(script)) {
                throw new FileNotFoundException(script.toString());
            }
        } catch (ParameterException e) {
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException(/* e.getMessage(), */ e);
        } catch (Exception e) {
            throw new RuntimeException("invalid script", e);
        }
        log.debug("config.script={}", script);
    }

    /**
     * get script.
     *
     * @return script file path
     */
    @Nonnull
    public Path getScript() {
        return this.script;
    }
}
