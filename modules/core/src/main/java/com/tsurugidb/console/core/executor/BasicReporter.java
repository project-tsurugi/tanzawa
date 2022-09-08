package com.tsurugidb.console.core.executor;

public class BasicReporter extends ScriptReporter {

    @Override
    public void info(String message) {
        System.out.println(message);
    }

    @Override
    public void succeed(String message) {
        System.out.println(message);
    }

    @Override
    public void warn(String message) {
        System.err.println(message);
    }
}
