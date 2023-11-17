package com.tsurugidb.console.core.parser;

enum TokenCategory {

    /**
     * Unknown tokens.
     */
    UNKNOWN,

    /**
     * Comments.
     */
    COMMENT,

    /**
     * Regular tokens.
     */
    REGULAR,

    /**
     * Punctuation.
     */
    PUNCTUATION,

    /**
     * Statement delimiters.
     */
    DELIMITER,

    /**
     * Pseudo tokens.
     */
    PSEUDO,
}
