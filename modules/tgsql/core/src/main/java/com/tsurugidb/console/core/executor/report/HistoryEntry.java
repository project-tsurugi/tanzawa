package com.tsurugidb.console.core.executor.report;

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
