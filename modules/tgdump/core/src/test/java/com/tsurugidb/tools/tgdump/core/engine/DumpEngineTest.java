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
package com.tsurugidb.tools.tgdump.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tools.common.monitoring.LoggingMonitor;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;

class DumpEngineTest {

    private static final Logger LOG = LoggerFactory.getLogger(DumpEngineTest.class);

    private final DumpMonitor monitor = new BasicDumpMonitor(new LoggingMonitor(getClass().getSimpleName(), LOG));

    private static DumpTarget target(String tableName) {
        return new DumpTarget(tableName, destination(tableName));
    }

    private static Path destination(String tableName) {
        return Path.of(tableName);
    }

    @Test
    void simple() throws Exception {
        var engine = new DumpEngine();
        var session = new MockDumpSession("T1");
        engine.execute(monitor, session, List.of(target("T1")));

        assertEquals(MockDumpSession.State.COMMITTED, session.getState());

        assertEquals(1, session.getRegistered().size());
        assertNotNull(session.getRegistered().get("T1"));

        assertEquals(1, session.getExecuted().size());
        assertEquals(destination("T1"), session.getExecuted().get("T1"));
    }

    @Test
    void multiple() throws Exception {
        var engine = new DumpEngine();
        var session = new MockDumpSession("T1", "T2", "T3");
        engine.execute(monitor, session, List.of(target("T1"), target("T2"), target("T3")));

        assertEquals(MockDumpSession.State.COMMITTED, session.getState());

        assertEquals(3, session.getRegistered().size());
        assertNotNull(session.getRegistered().get("T1"));
        assertNotNull(session.getRegistered().get("T2"));
        assertNotNull(session.getRegistered().get("T3"));

        assertEquals(3, session.getExecuted().size());
        assertEquals(destination("T1"), session.getExecuted().get("T1"));
        assertEquals(destination("T2"), session.getExecuted().get("T2"));
        assertEquals(destination("T3"), session.getExecuted().get("T3"));
    }

    @Test
    void multiple_threads() throws Exception {
        var engine = new DumpEngine(2);
        var session = new MockDumpSession("T1", "T2", "T3");
        engine.execute(monitor, session, List.of(target("T1"), target("T2"), target("T3")));

        assertEquals(MockDumpSession.State.COMMITTED, session.getState());

        assertEquals(3, session.getRegistered().size());
        assertNotNull(session.getRegistered().get("T1"));
        assertNotNull(session.getRegistered().get("T2"));
        assertNotNull(session.getRegistered().get("T3"));

        assertEquals(3, session.getExecuted().size());
        assertEquals(destination("T1"), session.getExecuted().get("T1"));
        assertEquals(destination("T2"), session.getExecuted().get("T2"));
        assertEquals(destination("T3"), session.getExecuted().get("T3"));
    }

    @Test
    void failure_missing_table() throws Exception {
        var engine = new DumpEngine();
        var session = new MockDumpSession("T1", "T2", "T3");
        engine.execute(monitor, session, List.of(target("T1"), target("T2"), target("T3")));

        assertEquals(MockDumpSession.State.COMMITTED, session.getState());

        assertEquals(3, session.getRegistered().size());
        assertNotNull(session.getRegistered().get("T1"));
        assertNotNull(session.getRegistered().get("T2"));
        assertNotNull(session.getRegistered().get("T3"));

        assertEquals(3, session.getExecuted().size());
        assertEquals(destination("T1"), session.getExecuted().get("T1"));
        assertEquals(destination("T2"), session.getExecuted().get("T2"));
        assertEquals(destination("T3"), session.getExecuted().get("T3"));
    }
}
