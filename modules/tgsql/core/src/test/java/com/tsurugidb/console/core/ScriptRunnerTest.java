package com.tsurugidb.console.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.executor.IoSupplier;
import com.tsurugidb.console.core.executor.engine.Engine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.model.ErroneousStatement;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.console.core.model.Statement;

class ScriptRunnerTest {

    static class Recorder implements Engine {

        private final ScriptConfig config = new ScriptConfig();
        final List<Statement> statements = new ArrayList<>();

        @Override
        public ScriptConfig getConfig() {
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
        var r = ScriptRunner.execute(script("SELECT * FROM T"), recorder);
        assertTrue(r);
        assertEquals(1, recorder.statements.size());
        assertEquals("SELECT * FROM T", recorder.text(Statement.Kind.GENERIC, 0));
    }

    @Test
    void raise() throws Exception {
        var r = ScriptRunner.execute(script("SELECT * FROM T"), new Engine() {
            private final ScriptConfig config = new ScriptConfig();

            @Override
            public ScriptConfig getConfig() {
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
        var r = ScriptRunner.execute(script("\\exit", "!!!"), recorder);
        assertTrue(r);
        assertEquals(1, recorder.statements.size());
        assertEquals("\\exit", recorder.text(Statement.Kind.SPECIAL, 0));
    }

    @Test
    void erroneous() throws Exception {
        Recorder recorder = new Recorder();
        var r = ScriptRunner.execute(script("COMMIT UNKNOWN"), recorder);
        assertTrue(r);
        assertEquals(1, recorder.statements.size());
        assertEquals("COMMIT UNKNOWN", recorder.text(Statement.Kind.ERRONEOUS, 0));
    }
}
