package com.tsurugidb.console.core.executor.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A basic implementation of {@link ScriptReporter}.
 */
public class BasicReporter extends ScriptReporter {
    private static final Logger LOG = LoggerFactory.getLogger(BasicReporter.class);

    @Override
    public void info(String message) {
        LOG.info(message);
    }

    @Override
    public void implicit(String message) {
        LOG.debug(message);
    }

    @Override
    public void succeed(String message) {
        LOG.info(message);
    }

    @Override
    public void warn(String message) {
        LOG.warn(message);
    }
}
