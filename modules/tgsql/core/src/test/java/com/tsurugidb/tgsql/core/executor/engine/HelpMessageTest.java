package com.tsurugidb.tgsql.core.executor.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class HelpMessageTest {

    static final Logger LOG = LoggerFactory.getLogger(HelpMessageTest.class);

    @Test
    void help() throws Exception {
        HelpMessage help = load();
        assertEquals(List.of("HELP", "A", "A B", "C"), help.find());
    }

    @Test
    void unrecognized() throws Exception {
        HelpMessage help = load();
        assertEquals(List.of("UNRECOGNIZED"), help.find(List.of("???")));
    }

    @Test
    void command() throws Exception {
        HelpMessage help = load();
        assertEquals(List.of("A"), help.find(List.of("a")));
    }

    @Test
    void command_multiple_words() throws Exception {
        HelpMessage help = load();
        assertEquals(List.of("AB"), help.find(List.of("a", "b")));
    }

    @Test
    void command_alias() throws Exception {
        HelpMessage help = load();
        assertEquals(List.of("A"), help.find(List.of("c")));
    }

    @Test
    void special() throws Exception {
        HelpMessage help = load();
        assertEquals(List.of("SA"), help.findForSpecialCommand("a"));
    }

    @Test
    void locale_unknown() throws Exception {
        HelpMessage help = HelpMessage.load("UNKNOWN_LOCALE");
        help.find().forEach(it -> LOG.debug("{}", it));
    }

    private static HelpMessage load() throws IOException {
        return HelpMessage.load("testing");
    }
}
