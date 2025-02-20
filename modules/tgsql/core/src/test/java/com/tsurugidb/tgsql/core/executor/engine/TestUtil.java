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
package com.tsurugidb.tgsql.core.executor.engine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Testing utilities.
 */
public final class TestUtil {

    /**
     * Reads file from test resources.
     * @param path the relative resource path from this package
     * @return the resource contents
     * @throws IOException if I/O error was occurred
     */
    public static String read(@Nonnull String path) throws IOException {
        var resource = TestUtil.class.getResource(path);
        if (resource == null) {
            throw new FileNotFoundException(path);
        }
        try (
            var input = resource.openStream();
            var reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            var writer = new StringWriter();
        ) {
            reader.transferTo(writer);
            return writer.toString();
        }
    }

    /**
     * Creates a new temporary directory.
     * @return the created directory
     * @throws IOException if I/O error was occurred
     */
    public static Path createTempDir() throws IOException {
        return Files.createTempDirectory("tanzawa-");
    }

    /**
     * Removes a directory including its contents.
     * @param directory the target directory
     * @throws IOException if I/O error was occurred
     */
    public static void removeDir(@Nullable Path directory) throws IOException {
        if (directory != null && Files.isDirectory(directory)) {
            Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc != null) {
                        throw exc;
                    }
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }


    private TestUtil() {
        throw new AssertionError();
    }
}
