package com.tsurugidb.console.cli.repl.jline;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class ReplJLineCompleterTest {

    @Test
    void testGetCandidateWord1() {
        String target = "\\help ";
        String candidate = "\\help";
        for (int len = 1; len <= target.length(); len++) {
            String input = target.substring(0, len);

            String expected;
            if (input.startsWith("\\help ")) {
                expected = null;
            } else {
                expected = "\\help";
            }
            testGetCandidateWord(input, candidate, expected);
        }
    }

    @Test
    void testGetCandidateWord1Unmatch() {
        String target = "\\halt ";
        String candidate = "\\help";
        for (int len = 1; len <= target.length(); len++) {
            String input = target.substring(0, len);

            String expected;
            if (input.startsWith("\\ha")) {
                expected = null;
            } else {
                expected = "\\help";
            }
            testGetCandidateWord(input, candidate, expected);
        }
    }

    @Test
    void testGetCandidateWord2() {
        String target = "\\show transaction ";
        String candidate = "\\show transaction";
        for (int len = 1; len <= target.length(); len++) {
            String input = target.substring(0, len);

            String expected;
            if (input.startsWith("\\show transaction ")) {
                expected = null;
            } else if (input.startsWith("\\show ")) {
                expected = "transaction";
            } else {
                expected = "\\show";
            }
            testGetCandidateWord(input, candidate, expected);
        }
    }

    @Test
    void testGetCandidateWord2Unmatch() {
        String target = "\\show table ";
        String candidate = "\\show transaction";
        for (int len = 1; len <= target.length(); len++) {
            String input = target.substring(0, len);

            String expected;
            if (input.startsWith("\\show ta")) {
                expected = null;
            } else if (input.startsWith("\\show ")) {
                expected = "transaction";
            } else {
                expected = "\\show";
            }
            testGetCandidateWord(input, candidate, expected);
        }
    }

    private static void testGetCandidateWord(String input, String candidate, String expected) {
        var inputWords = ReplJLineCompleter.toWordList(input);
        var candidateWords = List.of(candidate.split(" "));
        String actual = ReplJLineCompleter.getCandidateWord(inputWords, candidateWords);
        assertEquals(expected, actual, () -> "input=[" + input + "]");
    }
}
