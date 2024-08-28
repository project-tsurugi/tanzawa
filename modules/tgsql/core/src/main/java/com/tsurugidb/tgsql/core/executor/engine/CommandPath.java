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
package com.tsurugidb.tgsql.core.executor.engine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Represents a command path.
 */
public class CommandPath {

    static final Logger LOG = LoggerFactory.getLogger(CommandPath.class);

    static final boolean WINDOWS = System.getProperty("os.name", "") //$NON-NLS-1$ //$NON-NLS-2$
            .toLowerCase(Locale.ENGLISH)
            .startsWith("windows");

    static final String ENV_PATH = "PATH"; //$NON-NLS-1$

    static final String ENV_PATHEXT = "PATHEXT"; //$NON-NLS-1$

    static final List<String> WINDOWS_PATH_EXTENSIONS;
    static {
        var extensions = new ArrayList<String>();
        if (WINDOWS) {
            Optional.ofNullable(System.getenv(ENV_PATHEXT))
                    .map(it -> Arrays.stream(it.split(Pattern.quote(File.pathSeparator))))
                    .orElseGet(Stream::empty)
                    .sequential()
                    .map(String::trim)
                    .filter(it -> it.isEmpty() == false)
                    .map(it -> it.toLowerCase(Locale.ENGLISH))
                    .forEach(extensions::add);
        }
        WINDOWS_PATH_EXTENSIONS = extensions;
    }

    private final List<Path> directories;

    /**
     * Creates a new instance.
     * @param directories the path directories
     */
    public CommandPath(@Nonnull List<? extends Path> directories) {
        Objects.requireNonNull(directories);
        this.directories = List.copyOf(directories);
    }

    /**
     * Returns an appended path.
     * @param other to be appended in the tail of this path
     * @return the appended path
     */
    public CommandPath append(@Nonnull CommandPath other) {
        Objects.requireNonNull(other);
        var results = new ArrayList<Path>();
        results.addAll(directories);
        results.addAll(other.directories);
        return new CommandPath(results);
    }

    /**
     * Creates a new instance from environment variables.
     * @return the created instance
     */
    public static CommandPath system() {
        return CommandPath.of(System.getenv(ENV_PATH));
    }

    /**
     * Creates a new instance.
     * @param pathString the path list string
     * @return the created instance
     */
    public static CommandPath of(@Nullable String pathString) {
        if (pathString == null) {
            return new CommandPath(Collections.emptyList());
        }
        var directories = Stream.of(pathString.split(Pattern.quote(File.pathSeparator)))
            .map(String::trim)
            .filter(s -> s.isEmpty() == false)
            .map(Paths::get)
            .collect(Collectors.toList());
        return new CommandPath(directories);
    }

    /**
     * Returns a resolved file path of the specified command.
     * @param command the target command name
     * @return the command file path, or {@code empty} if there is no such a command
     */
    public Optional<Path> find(@Nonnull String command) {
        Objects.requireNonNull(command);
        return Stream.concat(
                    Stream.of(Paths.get(command)).filter(Path::isAbsolute),
                    directories.stream().map(d -> d.resolve(command)))
                .flatMap(CommandPath::expand)
                .filter(Files::isRegularFile)
                .filter(Files::isExecutable)
                .findFirst();
    }

    private static Stream<Path> expand(Path path) {
        if (WINDOWS) {
            String name = Optional.ofNullable(path.getFileName())
                    .map(Path::toString)
                    .orElse(null);
            if (name == null || name.lastIndexOf('.') >= 0) {
                // if already has extension, we don't add any extensions.
                return Stream.of(path);
            }
            // otherwise, we append each %PathExt% value.
            return WINDOWS_PATH_EXTENSIONS.stream()
                    .map(name::concat)
                    .map(path::resolveSibling);
        }
        return Stream.of(path);
    }

    /**
     * Returns the path string.
     * @return the path string
     */
    public String asPathString() {
        return directories.stream()
                .map(Path::toAbsolutePath)
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
    }

    @Override
    public String toString() {
        return directories.toString();
    }
}
