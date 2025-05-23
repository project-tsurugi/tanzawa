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
package com.tsurugidb.tgsql.core.executor.engine.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Completer candidate words.
 */
public class CompleterCandidateWords {

    private final List<String> wordList = new ArrayList<>(4);
    private final boolean end;

    /**
     * Creates a new instance.
     *
     * @param end whether to terminate
     */
    public CompleterCandidateWords(boolean end) {
        this.end = end;
    }

    /**
     * Creates a new instance.
     *
     * @param word candidate word
     * @param end  whether to terminate
     */
    public CompleterCandidateWords(String word, boolean end) {
        this(end);
        add(word);
    }

    /**
     * Creates a new instance.
     *
     * @param word1 candidate word
     * @param word2 candidate word
     * @param end   whether to terminate
     */
    public CompleterCandidateWords(String word1, String word2, boolean end) {
        this(end);
        add(word1);
        add(word2);
    }

    /**
     * Creates a new instance.
     *
     * @param word1 candidate word
     * @param word2 candidate word
     * @param word3 candidate word
     * @param end   whether to terminate
     */
    public CompleterCandidateWords(String word1, String word2, String word3, boolean end) {
        this(end);
        add(word1);
        add(word2);
        add(word3);
    }

    /**
     * add candidate word.
     *
     * @param words candidate word
     */
    public void add(List<String> words) {
        wordList.addAll(words);
    }

    /**
     * add candidate word.
     *
     * @param word candidate word
     */
    public void add(String word) {
        wordList.add(word);
    }

    /**
     * add candidate words.
     *
     * @param words      candidate words
     * @param startIndex index to start adding
     */
    public void addAll(List<String> words, int startIndex) {
        for (int i = startIndex; i < words.size(); i++) {
            String word = words.get(i);
            add(word);
        }
    }

    /**
     * get candidate words.
     *
     * @return candidate words
     */
    public List<String> getWords() {
        return this.wordList;
    }

    /**
     * get whether to terminate.
     *
     * @return whether to terminate
     */
    public boolean getEnd() {
        return this.end;
    }
}
