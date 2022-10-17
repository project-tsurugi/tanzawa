package com.tsurugidb.console.core.exception;

/**
 * script message exception.
 */
@SuppressWarnings("serial")
public class ScriptMessageException extends RuntimeException {

    /**
     * Creates a new instance.
     *
     * @param message the detail message
     */
    public ScriptMessageException(String message) {
        super(message);
    }

    /**
     * Creates a new instance.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public ScriptMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
