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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

class CommandPathTest {

    private Path temporary;

    @BeforeEach
    void prepareTempDir() throws IOException {
        temporary = TestUtil.createTempDir();
    }

    @AfterEach
    void cleanupTempDir() throws IOException {
        TestUtil.removeDir(temporary);
    }

    private static Path assumeWindowsCmd() {
        Assumptions.assumeTrue(CommandPath.WINDOWS, "assume windows");
        var path = CommandPath.system();
        return path.find("cmd.exe")
                .orElseThrow(() -> new TestAbortedException("should cmd.exe is available"));
    }

    private static Path assumeLinuxSh() {
        var path = CommandPath.system();
        return path.find("sh")
                .map(Path::toAbsolutePath)
                .filter(it -> Optional.ofNullable(it.getFileName())
                        .map(Path::toString)
                        .filter(name -> name.contains(".") == false) // skip "sh.exe"
                        .isPresent())
                .orElseThrow(() -> new TestAbortedException("should /bin/sh is available"));
    }

    private int launch(Path path, String... args) throws IOException, InterruptedException {
        var commandLine = new ArrayList<String>();
        commandLine.add(path.toString());
        commandLine.addAll(Arrays.asList(args));
        var process = new ProcessBuilder(commandLine)
                .directory(temporary.toFile())
                .start();
        try {
            assertTrue(process.waitFor(10, TimeUnit.SECONDS));
            return process.exitValue();
        } finally {
            process.destroyForcibly();
        }
    }

    @Test
    public void linux_sh() throws Exception {
        var sh = assumeLinuxSh();
        var exit = launch(sh, "-c", "touch result");
        assertEquals(0, exit);
        assertTrue(Files.exists(temporary.resolve("result")));
    }

    @Test
    public void linux_script() throws Exception {
        var sh = assumeLinuxSh();

        var script = temporary.resolve("script");
        Files.write(script, Arrays.asList(new String[] {
                "#!" + sh,
                "echo 'Hello, world!' > 'result'",
                "exit 0",
        }));
        Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwx------"));

        var path = new CommandPath(Arrays.asList(temporary));
        assertEquals(
                Optional.of(script.toAbsolutePath()),
                path.find("script").map(it -> it.toAbsolutePath()));
    }

    @Test
    void windows_cmd() throws Exception {
        var cmd = assumeWindowsCmd();

        var data = temporary.resolve("data");
        Files.write(data, Arrays.asList("Hello, world!"));

        var exit = launch(cmd, "/c", "copy data result");
        assertEquals(0, exit);
        assertTrue(Files.exists(temporary.resolve("result")));
    }

    @Test
    void windows_batch_extension() throws Exception {
        assumeWindowsCmd();

        var script = temporary.resolve("script.cmd");
        Files.write(script, Arrays.asList(new String[] {
                "echo Hello, world! > result",
        }));

        var path = new CommandPath(Arrays.asList(temporary));
        assertEquals(
                Optional.of(script.toAbsolutePath()),
                path.find("script").map(it -> it.toAbsolutePath()));
    }
}
