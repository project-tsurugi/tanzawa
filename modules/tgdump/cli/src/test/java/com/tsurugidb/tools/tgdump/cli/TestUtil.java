package com.tsurugidb.tools.tgdump.cli;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

final class TestUtil {

    static void delete(Path path) throws IOException {
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

    static Stream<JsonNode> readMonitor(Path monitorFile) throws IOException {
        var mapper = JsonMapper.builder()
                .build();
        var results = new ArrayList<JsonNode>();
        try (var reader = mapper.readerFor(JsonNode.class).<JsonNode>readValues(monitorFile.toFile())) {
            while (reader.hasNext()) {
                results.add(reader.nextValue());
            }
        }
        return results.stream();
    }


    private TestUtil() {
        throw new AssertionError();
    }
}
