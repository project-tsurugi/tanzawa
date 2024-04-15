package com.tsurugidb.tgsql.cli.repl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReplReporterTest {

    @Test
    void getResultSetSizeMessage() {
        var reporter = new ReplReporter();
        assertEquals("(0 rows)", reporter.getResultSetSizeMessage(0, false));
        assertEquals("(1 row)", reporter.getResultSetSizeMessage(1, false));
        assertEquals("(2 rows)", reporter.getResultSetSizeMessage(2, false));

        assertEquals("(0 rows over)", reporter.getResultSetSizeMessage(0, true));
        assertEquals("(1 row over)", reporter.getResultSetSizeMessage(1, true));
        assertEquals("(2 rows over)", reporter.getResultSetSizeMessage(2, true));
    }
}
