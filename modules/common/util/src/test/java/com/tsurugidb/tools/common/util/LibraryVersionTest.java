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
package com.tsurugidb.tools.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LibraryVersionTest {

    private Path tempDir;

    private Path createFile(Path relative) throws IOException {
        if (tempDir == null) {
            tempDir = Files.createTempDirectory(LibraryVersionTest.class.getSimpleName());
        }
        var file = tempDir.resolve(relative);
        var parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.createFile(file);
        return file;
    }

    private URLClassLoader createLoader() throws MalformedURLException {
        assertNotNull(tempDir);
        return new URLClassLoader(new URL[] { tempDir.toUri().toURL() });
    }

    @AfterEach
    void teardown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return super.visitFile(file, attrs);
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return super.postVisitDirectory(dir, exc);
                }
            });
        }
    }

    @Test
    void load() throws Exception {
        var file = createFile(Path.of("META-INF/tsurugidb/common-util-testing.properties"));
        Files.write(file, List.of(
                "Build-Jdk=11.0",
                "Build-OS=Tsurugidb",
                "Build-Revision=",
                "Build-Timestamp=2023-12-08T01\\:23\\:45.678+0900",
                "Build-Version=1.2.0",
                "Created-By=Gradle 8.3"));

        try (var loader = createLoader()) {
            var result = LibraryVersion.loadByName("common-util-testing", loader);
            System.out.println(result);
            assertEquals(Optional.of("1.2.0"), result.getBuildVersion());
            assertEquals(Optional.empty(), result.getBuildRevision());
            assertEquals(
                    Optional.of(OffsetDateTime.of(2023, 12, 8, 1, 23, 45, 678_000_000, ZoneOffset.of("+0900"))),
                    result.getBuildTimestamp());
            assertEquals(Optional.of("11.0"), result.getBuildJdk());
            assertEquals(Optional.of("Tsurugidb"), result.getBuildOs());
        }
    }

    @Test
    void load_not_found() throws Exception {
        assertThrows(FileNotFoundException.class, () -> LibraryVersion.loadByName("MISSING-LIBRARY", null));
    }
}
