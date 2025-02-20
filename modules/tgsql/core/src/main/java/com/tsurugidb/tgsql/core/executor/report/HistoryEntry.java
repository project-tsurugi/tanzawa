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
package com.tsurugidb.tgsql.core.executor.report;

/**
 * Entry of command history.
 */
public class HistoryEntry { // record

    private final int index;
    private final String text;

    /**
     * Creates a new instance.
     *
     * @param index index
     * @param text  text
     */
    public HistoryEntry(int index, String text) {
        this.index = index;
        this.text = text;
    }

    /**
     * get index.
     *
     * @return index
     */
    public int index() {
        return this.index;
    }

    /**
     * get text.
     *
     * @return text
     */
    public String text() {
        return this.text;
    }
}
