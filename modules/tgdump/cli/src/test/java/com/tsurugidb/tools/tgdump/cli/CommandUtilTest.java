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
package com.tsurugidb.tools.tgdump.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;
import com.tsurugidb.tools.tgdump.profile.ProfileDiagnosticCode;

class CommandUtilTest {

    private Path temporaryDir;

    Path getTemporaryDir() throws IOException {
        if (temporaryDir == null) {
            temporaryDir = Files.createTempDirectory(MainTest.class.getSimpleName());
        }
        return temporaryDir;
    }

    @AfterEach
    void teardown() throws IOException {
        if (temporaryDir != null) {
            TestUtil.delete(temporaryDir);
        }
    }

    @Test
    void loadProfile() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var profile = CommandUtil.loadProfile(args.getProfileBundleLoader(), Path.of("default"));
        assertEquals(Optional.of("default"), profile.getTitle());
    }

    @Test
    void loadProfile_missing_builtin() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var e = assertThrows(DiagnosticException.class,
                () -> CommandUtil.loadProfile(args.getProfileBundleLoader(), Path.of("missing")));
        assertEquals(ProfileDiagnosticCode.PROFILE_NOT_REGISTERED, e.getDiagnosticCode());
    }

    @Test
    void loadProfile_missing_file() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var e = assertThrows(DiagnosticException.class,
                () -> CommandUtil.loadProfile(args.getProfileBundleLoader(), Path.of("missing/profile.json")));
        assertEquals(ProfileDiagnosticCode.PROFILE_NOT_FOUND, e.getDiagnosticCode());
    }

    @Test
    void createMonitor() throws Exception {
        var root = getTemporaryDir();
        var file = root.resolve("monitor.jsonl");

        try (var monitor = CommandUtil.createMonitor(file)) {
            monitor.onStart();
            assertEquals(1, TestUtil.readMonitor(file).count());

            monitor.onSuccess();
            assertEquals(2, TestUtil.readMonitor(file).count());
        }
        assertEquals(2, TestUtil.readMonitor(file).count());
    }

    @Test
    void createMonitor_existing() throws Exception {
        var root = getTemporaryDir();
        var file = root.resolve("monitor.jsonl");
        Files.createFile(file);

        assertThrows(IOException.class, () -> CommandUtil.createMonitor(file));
    }

    @Test
    void prepareDestination() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var root = getTemporaryDir();
        var dest = root.resolve("destination");

        var targets = CommandUtil.prepareDestination(args.getTargetSelector(), dest, List.of("testing"));
        assertEquals(
                List.of(new DumpTarget("testing", dest.resolve("testing"))),
                targets);
    }

    @Test
    void prepareDestination_commands_empty() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var root = getTemporaryDir();
        var dest = root.resolve("destination");

        var e = assertThrows(DiagnosticException.class,
                () -> CommandUtil.prepareDestination(args.getTargetSelector(), dest, List.of(), false));
        assertEquals(CliDiagnosticCode.INVALID_PARAMETER, e.getDiagnosticCode());
    }

    @Test
    void prepareDestination_commands_invalid() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var root = getTemporaryDir();
        var dest = root.resolve("destination");

        var e = assertThrows(DiagnosticException.class,
                () -> CommandUtil.prepareDestination(args.getTargetSelector(), dest, List.of(""), false));
        assertEquals(CliDiagnosticCode.INVALID_PARAMETER, e.getDiagnosticCode());
    }

    @Test
    void prepareDestination_destination_relative() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var root = getTemporaryDir();
        var dest = root.resolve("destination");

        var cwd = Path.of(".").toAbsolutePath();
        var relativize = cwd.relativize(dest);

        var targets = CommandUtil.prepareDestination(args.getTargetSelector(), relativize, List.of("testing"));
        assertTrue(Files.isDirectory(dest));
        dest = dest.toRealPath();

        assertEquals(
                List.of(new DumpTarget("testing", dest.resolve("testing"))),
                targets);
    }

    @Test
    void prepareDestination_destination_existing() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var root = getTemporaryDir();
        var dest = root.resolve("destination");
        Files.createDirectories(dest);
        dest = dest.toRealPath();

        var targets = CommandUtil.prepareDestination(args.getTargetSelector(), dest, List.of("testing"));

        assertEquals(
                List.of(new DumpTarget("testing", dest.resolve("testing"))),
                targets);
    }

    @Test
    void prepareDestination_destination_failure() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var root = getTemporaryDir();
        var dest = root.resolve("destination");

        Files.createFile(dest);
        var e = assertThrows(DiagnosticException.class,
                () -> CommandUtil.prepareDestination(args.getTargetSelector(), dest, List.of("testing")));
        assertEquals(CliDiagnosticCode.DESTINATION_FAILURE, e.getDiagnosticCode());
    }

    @Test
    void prepareDestination_destination_exists_nonempty() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var root = getTemporaryDir();
        var dest = root.resolve("destination");
        Files.createDirectories(dest);
        Files.createFile(dest.resolve("existing"));
        var e = assertThrows(DiagnosticException.class,
                () -> CommandUtil.prepareDestination(args.getTargetSelector(), dest, List.of("testing")));
        assertEquals(CliDiagnosticCode.DESTINATION_EXISTS, e.getDiagnosticCode());
    }

    @Test
    void prepareDestination_single() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var root = getTemporaryDir();
        var dest = root.resolve("destination");

        var targets = CommandUtil.prepareDestination(args.getTargetSelector(), dest, List.of("testing"), true);
        assertEquals(
                List.of(new DumpTarget("testing", dest)),
                targets);
    }

    @Test
    void prepareDestination_single_multiple() throws Exception {
        CommandArgumentSet args = new CommandArgumentSet();
        var root = getTemporaryDir();
        var dest = root.resolve("destination");
        var e = assertThrows(DiagnosticException.class,
                () -> CommandUtil.prepareDestination(args.getTargetSelector(), dest, List.of("t1", "t2"), true));
        assertEquals(CliDiagnosticCode.INVALID_PARAMETER, e.getDiagnosticCode());
    }
}
