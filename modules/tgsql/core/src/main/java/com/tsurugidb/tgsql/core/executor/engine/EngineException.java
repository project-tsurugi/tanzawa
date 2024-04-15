package com.tsurugidb.tgsql.core.executor.engine;

import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * Errors in {@link Engine}.
 */
public class EngineException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance.
     */
    public EngineException() {
        super();
    }

    /**
     * Creates a new instance.
     * @param message the exception message
     */
    public EngineException(@Nullable String message) {
        super(message);
    }

    /**
     * Creates a new instance.
     * @param message the exception message
     * @param cause the original cause
     */
    public EngineException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
