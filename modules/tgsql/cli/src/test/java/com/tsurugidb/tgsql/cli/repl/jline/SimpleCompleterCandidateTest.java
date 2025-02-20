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
package com.tsurugidb.tgsql.cli.repl.jline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class SimpleCompleterCandidateTest {

    @Test
    void testFindCandidateWord1() {
        String target = "\\help ";
        String candidateLine = "\\help";
        boolean candidateEnd = false;
        for (int len = 1; len <= target.length(); len++) {
            String input = target.substring(0, len);

            String expectedWord;
            boolean expectedEnd;
            if (input.startsWith("\\help ")) {
                expectedWord = null;
                expectedEnd = candidateEnd;
            } else {
                expectedWord = "\\help";
                expectedEnd = candidateEnd;
            }
            testFindCandidateWord(input, candidateLine, candidateEnd, expectedWord, expectedEnd);
        }
    }

    @Test
    void testFindCandidateWord1Unmatch() {
        String target = "\\halt ";
        String candidateLine = "\\help";
        boolean candidateEnd = false;
        for (int len = 1; len <= target.length(); len++) {
            String input = target.substring(0, len);

            String expectedWord;
            boolean expectedEnd;
            if (input.startsWith("\\ha")) {
                expectedWord = null;
                expectedEnd = candidateEnd;
            } else {
                expectedWord = "\\help";
                expectedEnd = candidateEnd;
            }
            testFindCandidateWord(input, candidateLine, candidateEnd, expectedWord, expectedEnd);
        }
    }

    @Test
    void testFindCandidateWord2() {
        String target = "\\show table ";
        String candidateLine = "\\show table";
        boolean candidateEnd = false;
        for (int len = 1; len <= target.length(); len++) {
            String input = target.substring(0, len);

            String expectedWord;
            boolean expectedEnd;
            if (input.startsWith("\\show table ")) {
                expectedWord = null;
                expectedEnd = candidateEnd;
            } else if (input.startsWith("\\show ")) {
                expectedWord = "table";
                expectedEnd = candidateEnd;
            } else {
                expectedWord = "\\show";
                expectedEnd = false;
            }
            testFindCandidateWord(input, candidateLine, candidateEnd, expectedWord, expectedEnd);
        }
    }

    @Test
    void testFindCandidateWord2End() {
        String target = "\\show transaction ";
        String candidateLine = "\\show transaction";
        boolean candidateEnd = true;
        for (int len = 1; len <= target.length(); len++) {
            String input = target.substring(0, len);

            String expectedWord;
            boolean expectedEnd;
            if (input.startsWith("\\show transaction ")) {
                expectedWord = null;
                expectedEnd = candidateEnd;
            } else if (input.startsWith("\\show ")) {
                expectedWord = "transaction";
                expectedEnd = candidateEnd;
            } else {
                expectedWord = "\\show";
                expectedEnd = false;
            }
            testFindCandidateWord(input, candidateLine, candidateEnd, expectedWord, expectedEnd);
        }
    }

    @Test
    void testFindCandidateWord2EndUnmatch() {
        String target = "\\show table ";
        String candidateLine = "\\show transaction";
        boolean candidateEnd = true;
        for (int len = 1; len <= target.length(); len++) {
            String input = target.substring(0, len);

            String expectedWord;
            boolean expectedEnd;
            if (input.startsWith("\\show ta")) {
                expectedWord = null;
                expectedEnd = candidateEnd;
            } else if (input.startsWith("\\show ")) {
                expectedWord = "transaction";
                expectedEnd = candidateEnd;
            } else {
                expectedWord = "\\show";
                expectedEnd = false;
            }
            testFindCandidateWord(input, candidateLine, candidateEnd, expectedWord, expectedEnd);
        }
    }

    private static void testFindCandidateWord(String input, String candidateLine, boolean candidateEnd, String expectedWord, boolean expectedEnd) {
        var inputWords = SimpleCompleterCandidate.toWordList(input);
        var candidate = new SimpleCompleterCandidate(candidateLine, candidateEnd);
        var actual = candidate.findCandidateWord(inputWords);
        if (expectedWord == null) {
            assertNull(actual, () -> "input=[" + input + "]");
        } else {
            assertEquals(expectedWord, actual.word(), () -> "input=[" + input + "]");
            assertEquals(expectedEnd, actual.end(), () -> "input=[" + input + "]");
        }
    }
}
