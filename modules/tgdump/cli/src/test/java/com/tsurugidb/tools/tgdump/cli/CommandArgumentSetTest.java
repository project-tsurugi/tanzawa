package com.tsurugidb.tools.tgdump.cli;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class CommandArgumentSetTest {

    @Test
    void getProfileReader() {
        var args = new CommandArgumentSet();
        assertNotNull(args.getProfileReader());
    }

    @Test
    void getProfileBundleLoader() {
        var args = new CommandArgumentSet();
        assertNotNull(args.getProfileBundleLoader());
    }

    @Test
    void getTargetSelector() {
        var args = new CommandArgumentSet();
        assertNotNull(args.getTargetSelector());
    }

    @Test
    void getConnectionProvider() {
        var args = new CommandArgumentSet();
        assertNotNull(args.getConnectionProvider());
    }
}
