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
package com.tsurugidb.tools.tgdump.core.engine;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Assertions;

import com.tsurugidb.tools.common.monitoring.MonitoringException;
import com.tsurugidb.tsubakuro.sql.TableMetadata;

/**
 * Mock implementation of {@link DumpMonitor}.
 */
public class MockDumpMonitor implements DumpMonitor {

    private final Map<String, Path> info = new ConcurrentHashMap<>();

    private final Map<String, List<Path>> files = new ConcurrentHashMap<>();

    private final Map<String, Path> start = new ConcurrentHashMap<>();

    private final Map<String, Path> finish = new ConcurrentHashMap<>();

    /**
     * Returns the info monitored.
     * @return the info monitored
     */
    public Map<String, Path> getInfo() {
        return info;
    }

    /**
     * Returns the files monitored.
     * @return the files monitored
     */
    public Map<String, List<Path>> getFiles() {
        return files;
    }

    /**
     * Returns the start monitored.
     * @return the start monitored
     */
    public Map<String, Path> getStart() {
        return start;
    }

    /**
     * Returns the finish monitored.
     * @return the finish monitored
     */
    public Map<String, Path> getFinish() {
        return finish;
    }

    @Override
    public void verbose(String format, Object... arguments) throws MonitoringException {
        System.out.println(MessageFormat.format(format, arguments));
    }

    @Override
    public void onDumpInfo(String tableName, TableMetadata tableInfo, Path dumpDirectory) throws MonitoringException {
        var prev = info.putIfAbsent(tableName, dumpDirectory);
        Assertions.assertNull(prev);
    }

    @Override
    public void onDumpInfo(String label, String query, Path dumpDirectory) throws MonitoringException {
        var prev = info.putIfAbsent(label, dumpDirectory);
        Assertions.assertNull(prev);
    }

    @Override
    public void onDumpStart(String tableName, Path dumpDirectory) throws MonitoringException {
        var prev = start.putIfAbsent(tableName, dumpDirectory);
        Assertions.assertNull(prev);
    }

    @Override
    public void onDumpFile(String tableName, Path dumpFile) throws MonitoringException {
        files.computeIfAbsent(tableName, it -> new ArrayList<>()).add(dumpFile);
    }

    @Override
    public void onDumpFinish(String tableName, Path dumpDirectory) throws MonitoringException {
        var prev = finish.putIfAbsent(tableName, dumpDirectory);
        Assertions.assertNull(prev);
    }
}
