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
package com.tsurugidb.tgsql.core.model;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * A {@link Statement} that consists of special commands.
 */
public class SpecialStatement implements Statement {

    private final String text;

    private final Region region;

    private final Regioned<String> commandName;

    private final List<Regioned<String>> commandOptions;

    /**
     * Creates a new instance.
     * @param text the text of this statement
     * @param region the region of this statement in the document
     * @param commandName the command name, must does not start back-slash
     */
    public SpecialStatement(@Nonnull String text, @Nonnull Region region, @Nonnull Regioned<String> commandName) {
        this(text, region, commandName, List.of());
    }

    /**
     * Creates a new instance.
     * @param text the text of this statement
     * @param region the region of this statement in the document
     * @param commandName the command name, must does not start back-slash
     * @param commandOptions the command options
     */
    public SpecialStatement(
            @Nonnull String text,
            @Nonnull Region region,
            @Nonnull Regioned<String> commandName,
            @Nonnull List<Regioned<String>> commandOptions) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(region);
        Objects.requireNonNull(commandName);
        Objects.requireNonNull(commandOptions);
        this.text = text;
        this.region = region;
        this.commandName = commandName;
        this.commandOptions = List.copyOf(commandOptions);
    }

    @Override
    public Kind getKind() {
        return Kind.SPECIAL;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    /**
     * Returns the command name (without leading back-slash).
     * @return the command name
     */
    public Regioned<String> getCommandName() {
        return commandName;
    }

    /**
     * Returns the command options.
     * @return the command options
     */
    public List<Regioned<String>> getCommandOptions() {
        return commandOptions;
    }

    @Override
    public String toString() {
        return String.format(
                "Statement(kind=%s, text='%s', region=%s, commandName=%s, commandOptions=%s)", //$NON-NLS-1$
                getKind(),
                text,
                region,
                commandName,
                commandOptions);
    }
}
