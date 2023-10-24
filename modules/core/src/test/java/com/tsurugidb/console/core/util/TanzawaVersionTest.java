package com.tsurugidb.console.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.junit.jupiter.api.Test;

class TanzawaVersionTest {

    @Test
    void getBuildTimestamp() throws IOException {
        String version = TanzawaVersion.getBuildTimestamp("version-test");
        assertEquals("2023-10-24T12:52:36.203+0900", version);
    }

    @Test
    void getBuildRevision() throws IOException {
        String version = TanzawaVersion.getBuildRevision("version-test");
        assertEquals("abcdefg", version);
    }

    @Test
    void getBuildVersion() throws IOException {
        String version = TanzawaVersion.getBuildVersion("version-test");
        assertEquals("1.0.0-TEST", version);
    }

    @Test
    void notFoundVersionFile() {
        var e = assertThrows(IOException.class, () -> {
            TanzawaVersion.getProperties("not-found");
        });
        assertEquals("version file load error. module=tanzawa-not-found", e.getMessage());
        var c = (FileNotFoundException) e.getCause();
        assertEquals("missing version file. path=/META-INF/tsurugidb/tanzawa-not-found.properties", c.getMessage());
    }
}
