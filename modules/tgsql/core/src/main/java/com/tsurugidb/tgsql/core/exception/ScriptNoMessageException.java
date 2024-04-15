package com.tsurugidb.tgsql.core.exception;

/**
 * script no message exception.
 */
@SuppressWarnings("serial")
public class ScriptNoMessageException extends RuntimeException {

    /**
     * Creates a new instance.
     *
     * @param cause the cause
     */
    public ScriptNoMessageException(Throwable cause) {
        super(cause);
    }
}
