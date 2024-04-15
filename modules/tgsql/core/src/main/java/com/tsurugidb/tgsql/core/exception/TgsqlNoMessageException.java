package com.tsurugidb.tgsql.core.exception;

/**
 * script no message exception.
 */
@SuppressWarnings("serial")
public class TgsqlNoMessageException extends RuntimeException {

    /**
     * Creates a new instance.
     *
     * @param cause the cause
     */
    public TgsqlNoMessageException(Throwable cause) {
        super(cause);
    }
}
