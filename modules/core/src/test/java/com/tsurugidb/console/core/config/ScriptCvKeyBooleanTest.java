package com.tsurugidb.console.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.tsurugidb.console.core.config.ScriptCvKey.ScriptCvKeyBoolean;

class ScriptCvKeyBooleanTest {

    @Test
    void convertValue() {
        var key = new ScriptCvKeyBoolean("test");
        assertEquals(Boolean.FALSE, key.convertValue(""));
        assertEquals(Boolean.FALSE, key.convertValue(" "));

        assertEquals(Boolean.TRUE, key.convertValue("t"));
        assertEquals(Boolean.TRUE, key.convertValue("tr"));
        assertEquals(Boolean.TRUE, key.convertValue("tru"));
        assertEquals(Boolean.TRUE, key.convertValue("true"));
        assertEquals(Boolean.FALSE, key.convertValue("truee"));

        assertEquals(Boolean.TRUE, key.convertValue("T"));
        assertEquals(Boolean.TRUE, key.convertValue("TR"));
        assertEquals(Boolean.TRUE, key.convertValue("TRU"));
        assertEquals(Boolean.TRUE, key.convertValue("TRUE"));
        assertEquals(Boolean.FALSE, key.convertValue("TRUEE"));

        assertEquals(Boolean.TRUE, key.convertValue("1"));
        assertEquals(Boolean.FALSE, key.convertValue("0"));
    }
}
