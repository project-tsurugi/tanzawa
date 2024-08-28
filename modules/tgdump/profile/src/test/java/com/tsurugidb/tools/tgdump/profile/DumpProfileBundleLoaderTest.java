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
package com.tsurugidb.tools.tgdump.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.tgdump.core.model.ArrowFileFormat;
import com.tsurugidb.tools.tgdump.core.model.DumpProfile;
import com.tsurugidb.tools.tgdump.core.model.ParquetFileFormat;

class DumpProfileBundleLoaderTest {

    private Path temp;

    private final List<Path> escaped = new ArrayList<>();

    private final DumpProfileReader reader = new DumpProfileReader(JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build());

    private Path touch(String relativePath) throws IOException {
        if (temp == null) {
            temp = Files.createTempDirectory(getClass().getSimpleName());
        }
        var path = temp.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.createFile(path);
        return path;
    }

    private void escapeTempDir() {
        if (temp != null) {
            escaped.add(temp);
            temp = null;
        }
    }

    private URLClassLoader createClassLoader() throws IOException {
        escapeTempDir();
        var urls = new ArrayList<URL>();
        for (var path : escaped) {
            urls.add(path.toUri().toURL());
        }
        return new URLClassLoader(urls.toArray(new URL[urls.size()]));
    }

    private static final Map<String, String> VARS = Map.of(
            "format_version", String.valueOf(DumpProfileReader.FORMAT_VERSION));

    private static final Pattern PATTERN_VARIABLE = Pattern.compile("\\{(\\w+)\\}");

    private static Path prepare(Path file, String... lines) throws IOException {
        var candidate = Arrays.stream(lines)
                .map(DumpProfileBundleLoaderTest::extractVariables)
                .collect(Collectors.toList());
        Files.write(file, candidate);
        return file;
    }

    private static String extractVariables(String source) {
        // replace {<k>} -> variables.get(k)
        var results = new StringBuilder();
        var m = PATTERN_VARIABLE.matcher(source);
        int start;
        for (start = 0; m.find(start); start = m.end()) {
            results.append(source.substring(start, m.start()));
            var key = m.group(1);
            var value = VARS.get(key);
            if (value != null) {
                results.append(value);
            } else {
                throw new IllegalArgumentException(key);
            }
        }
        results.append(source.substring(start));
        return results.toString();
    }


    @AfterEach
    void teardown() throws IOException {
        escapeTempDir();
        for (Path path : escaped) {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
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

    private static DumpProfile profile(String title) {
        return DumpProfile.newBuilder()
                .withTitle(title)
                .build();
    }

    private static String[] json(String title) {
        return new String[] {
                "{",
                "  'format_version': {format_version},",
                String.format("  'title': '%s'", title),
                "}",
        };
    }

    @Test
    void simple() throws Exception {
        prepare(touch("index.properties"), new String[] {
                "testing=testing.json",
        });
        prepare(touch("testing.json"), new String[] {
                "{",
                "  'format_version': {format_version},",
                "}",
        });
        try (var cl = createClassLoader()) {
            var loader = new DumpProfileBundleLoader(reader, cl, false);
            var bundle = loader.load("index.properties");
            assertEquals(new DumpProfileBundle(Map.of("testing", new DumpProfile())), bundle);
        }
    }

    @Test
    void nothing() throws Exception {
        touch("dummy.properties");
        try (var cl = createClassLoader()) {
            var loader = new DumpProfileBundleLoader(reader, cl, false);
            var bundle = loader.load("index.properties");
            assertEquals(new DumpProfileBundle(), bundle);
        }
    }

    @Test
    void multiple_profiles() throws Exception {
        prepare(touch("index.properties"), new String[] {
                "p1=p1.json",
                "p2=p2.json",
                "p3=p3.json",
        });
        prepare(touch("p1.json"), json("p1"));
        prepare(touch("p2.json"), json("p2"));
        prepare(touch("p3.json"), json("p3"));
        try (var cl = createClassLoader()) {
            var loader = new DumpProfileBundleLoader(reader, cl, false);
            var bundle = loader.load("index.properties");
            assertEquals(
                    new DumpProfileBundle(Map.of(
                            "p1", profile("p1"),
                            "p2", profile("p2"),
                            "p3", profile("p3"))),
                    bundle);
        }
    }

    @Test
    void multiple_indices() throws Exception {
        prepare(touch("index.properties"), new String[] {
                "p1=p1.json",
        });
        prepare(touch("p1.json"), json("p1"));
        escapeTempDir();

        prepare(touch("index.properties"), new String[] {
                "p2=p2.json",
        });
        prepare(touch("p2.json"), json("p2"));
        escapeTempDir();

        prepare(touch("index.properties"), new String[] {
                "p3=p3.json",
        });
        prepare(touch("p3.json"), json("p3"));
        escapeTempDir();

        try (var cl = createClassLoader()) {
            var loader = new DumpProfileBundleLoader(reader, cl, false);
            var bundle = loader.load("index.properties");
            assertEquals(
                    new DumpProfileBundle(Map.of(
                            "p1", profile("p1"),
                            "p2", profile("p2"),
                            "p3", profile("p3"))),
                    bundle);
        }
    }

    @Test
    void missing_profile() throws Exception {
        prepare(touch("index.properties"), new String[] {
                "x=MISSING",
        });
        try (var cl = createClassLoader()) {
            var loader = new DumpProfileBundleLoader(reader, cl, false);
            var e = assertThrows(DiagnosticException.class, () -> loader.load("index.properties"));
            assertEquals(ProfileDiagnosticCode.PROFILE_NOT_FOUND, e.getDiagnosticCode());
        }
    }

    @Test
    void skip_missing_profile() throws Exception {
        prepare(touch("index.properties"), new String[] {
                "p1=p1.json",
                "p2=MISSING",
                "p3=p3.json",
        });
        prepare(touch("p1.json"), json("p1"));
        prepare(touch("p2.json"), json("p2"));
        prepare(touch("p3.json"), json("p3"));
        try (var cl = createClassLoader()) {
            var loader = new DumpProfileBundleLoader(reader, cl, true);
            var bundle = loader.load("index.properties");
            assertEquals(
                    new DumpProfileBundle(Map.of(
                            "p1", profile("p1"),
                            "p3", profile("p3"))),
                    bundle);
        }
    }

    @Test
    void duplicate_labels() throws Exception {
        prepare(touch("index.properties"), new String[] {
                "p=p1.json",
        });
        prepare(touch("p1.json"), json("p1"));
        escapeTempDir();

        prepare(touch("index.properties"), new String[] {
                "p=p2.json",
        });
        prepare(touch("p2.json"), json("p2"));
        escapeTempDir();

        try (var cl = createClassLoader()) {
            var loader = new DumpProfileBundleLoader(reader, cl, false);
            var bundle = loader.load("index.properties");
            assertTrue(
                    Set.of(
                            new DumpProfileBundle(Map.of("p", profile("p1"))),
                            new DumpProfileBundle(Map.of("p", profile("p2"))))
                    .contains(bundle),
                    String.valueOf(bundle));
        }
    }

    @Test
    void duplicate_files() throws Exception {
        prepare(touch("index.properties"), new String[] {
                "p=p1.json",
        });
        prepare(touch("p1.json"), json("p1"));
        escapeTempDir();

        prepare(touch("p1.json"), json("p1"));
        escapeTempDir();

        prepare(touch("p1.json"), json("p2"));
        escapeTempDir();

        try (var cl = createClassLoader()) {
            var loader = new DumpProfileBundleLoader(reader, cl, false);
            var bundle = loader.load("index.properties");
            assertTrue(
                    Set.of(
                            new DumpProfileBundle(Map.of("p", profile("p1"))),
                            new DumpProfileBundle(Map.of("p", profile("p2"))))
                    .contains(bundle),
                    String.valueOf(bundle));
        }
    }

    @Test
    void system_default() throws Exception {
        var loader = new DumpProfileBundleLoader(reader, DumpProfileBundleLoader.class.getClassLoader(), false);
        var bundle = loader.load();
        var profile = bundle.getProfile("default");
        assertEquals(Optional.of("default"), profile.getTitle());
        assertNotEquals(Optional.empty(), profile.getDescription());
        assertEquals(Optional.empty(), profile.getFileFormat());
    }

    @Test
    void system_parquet() throws Exception {
        var loader = new DumpProfileBundleLoader(reader, DumpProfileBundleLoader.class.getClassLoader(), false);
        var bundle = loader.load();
        var profile = bundle.getProfile("parquet");
        assertEquals(Optional.of("Parquet"), profile.getTitle());
        assertNotEquals(Optional.empty(), profile.getDescription());
        assertEquals(Optional.of(new ParquetFileFormat()), profile.getFileFormat());
    }

    @Test
    void system_arrow() throws Exception {
        var loader = new DumpProfileBundleLoader(reader, DumpProfileBundleLoader.class.getClassLoader(), false);
        var bundle = loader.load();
        var profile = bundle.getProfile("arrow");
        assertEquals(Optional.of("Arrow"), profile.getTitle());
        assertNotEquals(Optional.empty(), profile.getDescription());
        assertEquals(Optional.of(new ArrowFileFormat()), profile.getFileFormat());
    }

    @Test
    void system_pg_strom() throws Exception {
        var loader = new DumpProfileBundleLoader(reader, DumpProfileBundleLoader.class.getClassLoader(), false);
        var bundle = loader.load();
        var profile = bundle.getProfile("pg-strom");
        assertEquals(Optional.of("PG-Strom"), profile.getTitle());
        assertNotEquals(Optional.empty(), profile.getDescription());
        assertEquals(
                Optional.of(ArrowFileFormat.newBuilder()
                        .withRecordBatchInBytes(256L * 1024 * 1024)
                        .withCharacterFieldType(ArrowFileFormat.CharacterFieldType.FIXED_SIZE_BINARY)
                        .build()),
                profile.getFileFormat());
    }
}
