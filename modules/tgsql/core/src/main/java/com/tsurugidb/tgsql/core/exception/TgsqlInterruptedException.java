package com.tsurugidb.tgsql.core.exception;

/**
 * user interrupted exception.
 */
@SuppressWarnings("serial")
public class TgsqlInterruptedException extends RuntimeException {

    /**
     * Creates a new instance.
     * 
     * @param cause the cause
     */
    public TgsqlInterruptedException(Throwable cause) {
        super(cause);
    }
}
