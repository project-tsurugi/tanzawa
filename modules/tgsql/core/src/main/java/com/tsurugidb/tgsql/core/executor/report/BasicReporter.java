package com.tsurugidb.tgsql.core.executor.report;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.config.TgsqlConfig;

/**
 * A basic implementation of {@link TgsqlReporter}.
 */
public class BasicReporter extends TgsqlReporter {
    private static final Logger LOG = LoggerFactory.getLogger(BasicReporter.class);

    /**
     * Creates a new instance.
     *
     * @param config tgsql configuration
     */
    public BasicReporter(@Nonnull TgsqlConfig config) {
        super(config);
    }

    @Override
    protected void doInfo(String message) {
        LOG.info(message);
    }

    @Override
    protected void doImplicit(String message) {
        LOG.debug(message);
    }

    @Override
    protected void doSucceed(String message) {
        LOG.info(message);
    }

    @Override
    protected void doWarn(String message) {
        LOG.warn(message);
    }
}
