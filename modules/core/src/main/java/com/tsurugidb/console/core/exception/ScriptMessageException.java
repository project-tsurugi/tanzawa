package com.tsurugidb.console.core.exception;

/**
 * script message exception.
 */
@SuppressWarnings("serial")
public class ScriptMessageException extends RuntimeException {

    private long timingTime;

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

    /**
     * Creates a new instance.
     *
     * @param message the detail message
     * @param cause   the cause
     * @param time    time
     */
    public ScriptMessageException(String message, Throwable cause, long time) {
        super(message, cause);
        this.timingTime = time;
    }

    /**
     * get time.
     *
     * @return time
     */
    public long getTimingTime() {
        return this.timingTime;
    }
}
