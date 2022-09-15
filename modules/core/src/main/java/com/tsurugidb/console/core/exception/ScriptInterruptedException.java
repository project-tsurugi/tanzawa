package com.tsurugidb.console.core.exception;

/**
 * user interrupted exception.
 */
@SuppressWarnings("serial")
public class ScriptInterruptedException extends RuntimeException {

    /**
     * Creates a new instance.
     * 
     * @param cause the cause
     */
    public ScriptInterruptedException(Throwable cause) {
        super(cause);
    }
}
