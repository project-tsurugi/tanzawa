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
package com.tsurugidb.tgsql.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.executor.IoSupplier;
import com.tsurugidb.tgsql.core.executor.engine.Engine;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.model.ErroneousStatement;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
import com.tsurugidb.tgsql.core.model.Statement;

class TgsqlRunnerTest {

    static class Recorder implements Engine {

        private final TgsqlConfig config = new TgsqlConfig();
        final List<Statement> statements = new ArrayList<>();

        @Override
        public TgsqlConfig getConfig() {
            return this.config;
        }

        @Override
        public boolean execute(Statement statement) throws EngineException {
            statements.add(statement);
            if (statement instanceof SpecialStatement || statement instanceof ErroneousStatement) {
                return false;
            }
            return true;
        }

        String text(Statement.Kind expected, int index) {
            Statement occurred = statements.get(index);
            assertEquals(expected, occurred.getKind());
            return occurred.getText().trim();
        }
    }

    private static IoSupplier<? extends Reader> script(String... lines) {
        return () -> new StringReader(String.join("\n", lines));
    }

    @Test
    void simple() throws Exception {
        Recorder recorder = new Recorder();
        var r = TgsqlRunner.execute(script("SELECT * FROM T"), recorder);
        assertTrue(r);
        assertEquals(1, recorder.statements.size());
        assertEquals("SELECT * FROM T", recorder.text(Statement.Kind.GENERIC, 0));
    }

    @Test
    void raise() throws Exception {
        var r = TgsqlRunner.execute(script("SELECT * FROM T"), new Engine() {
            private final TgsqlConfig config = new TgsqlConfig();

            @Override
            public TgsqlConfig getConfig() {
                return config;
            }

            @Override
            public boolean execute(Statement statement) throws EngineException {
                throw new EngineException("TESTING");
            }
        });
        assertFalse(r);
    }

    @Test
    void special() throws Exception {
        Recorder recorder = new Recorder();
        var r = TgsqlRunner.execute(script("\\exit", "!!!"), recorder);
        assertTrue(r);
        assertEquals(1, recorder.statements.size());
        assertEquals("\\exit", recorder.text(Statement.Kind.SPECIAL, 0));
    }

    @Test
    void erroneous() throws Exception {
        Recorder recorder = new Recorder();
        var r = TgsqlRunner.execute(script("COMMIT UNKNOWN"), recorder);
        assertTrue(r);
        assertEquals(1, recorder.statements.size());
        assertEquals("COMMIT UNKNOWN", recorder.text(Statement.Kind.ERRONEOUS, 0));
    }
}
