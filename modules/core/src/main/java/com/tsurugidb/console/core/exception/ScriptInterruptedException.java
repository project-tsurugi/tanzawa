package com.tsurugidb.console.core.exception;

@SuppressWarnings("serial")
public class ScriptInterruptedException extends RuntimeException {

    public ScriptInterruptedException(Throwable cause) {
        super(cause);
    }
}
