package com.tsurugidb.console.cli.repl.jline;

import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

/**
 * Simple completer candidate.
 */
public class SimpleCompleterCandidate {

    private final List<String> candidateWords;
    private final boolean end;

    /**
     * Creates a new instance.
     * 
     * @param candidateWords candidate words
     * @param end            whether to terminate
     */
    public SimpleCompleterCandidate(List<String> candidateWords, boolean end) {
        this.candidateWords = candidateWords;
        this.end = end;
    }

    /**
     * Creates a new instance.
     * 
     * @param candidateLine candidate words
     * @param end           whether to terminate
     */
    public SimpleCompleterCandidate(String candidateLine, boolean end) {
        this(List.of(toWordList(candidateLine)), end);
    }

    /**
     * Split on whitespace.
     * 
     * @param line text
     * @return words
     */
    public static String[] toWordList(String line) {
        String[] words = line.toLowerCase(Locale.ENGLISH).split("[ \t\r\n]+", -1); //$NON-NLS-1$
        return words;
    }

    /**
     * Candidate word.
     */
    public static final class CandidateWord {
        private final String word;
        private final boolean end;

        CandidateWord(String word, boolean end) {
            this.word = word;
            this.end = end;
        }

        /**
         * get candidate word.
         * 
         * @return candidate word
         */
        public String word() {
            return this.word;
        }

        /**
         * get whether to terminate.
         * 
         * @return whether to terminate
         */
        public boolean end() {
            return this.end;
        }
    }

    /**
     * get candidate word.
     * 
     * @param inputWords input words
     * @return candidate word. null if no candidate
     */
    @Nullable
    public CandidateWord findCandidateWord(String[] inputWords) {
        if (inputWords.length > candidateWords.size()) {
            return null;
        }

        for (int i = 0; i < inputWords.length; i++) {
            String inputWord = inputWords[i];
            String candidate = candidateWords.get(i);
            if (inputWord.equals(candidate)) {
                if (i == inputWords.length - 1) {
                    return new CandidateWord(candidate, isEnd(i));
                }
                continue;
            }

            if (i == inputWords.length - 1) {
                if (candidate.startsWith(inputWord)) {
                    return new CandidateWord(candidate, isEnd(i));
                }
            }
            return null;
        }

        int i = inputWords.length;
        if (i < candidateWords.size()) {
            return new CandidateWord(candidateWords.get(i), isEnd(i));
        }
        return null;
    }

    private boolean isEnd(int i) {
        if (i == candidateWords.size() - 1) {
            return this.end;
        }
        return false;
    }
}
