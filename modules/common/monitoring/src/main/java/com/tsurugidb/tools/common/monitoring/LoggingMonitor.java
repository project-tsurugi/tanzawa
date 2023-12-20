package com.tsurugidb.tools.common.monitoring;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.value.Property;

/**
 * A {@link Monitor} that output monitoring information to a logger.
 */
public class LoggingMonitor implements Monitor {

    private final String application;

    private final Logger logger;

    /**
     * Creates a new instance.
     * @param application the application name
     * @param logger the destination logger
     */
    public LoggingMonitor(@Nonnull String application, @Nonnull Logger logger) {
        Objects.requireNonNull(application);
        Objects.requireNonNull(logger);
        this.application = application;
        this.logger = logger;
    }

    @Override
    public void onStart() throws MonitoringException {
        logger.info("start: application={}", application); //$NON-NLS-1$
    }

    @Override
    public void onData(@Nonnull String format, @Nonnull List<? extends Property> properties) {
        Objects.requireNonNull(format);
        Objects.requireNonNull(properties);
        logger.info("{}: application={}, properties={}", format, application, properties); //$NON-NLS-1$
    }

    @Override
    public void onSuccess() {
        logger.info("success: application={}", application); //$NON-NLS-1$
    }

    @Override
    public void onFailure(@Nullable Throwable cause, @Nonnull DiagnosticCode code, @Nonnull List<?> arguments) {
        Objects.requireNonNull(code);
        Objects.requireNonNull(arguments);
        if (cause == null) {
            logger.info("failure: application={}, reason={}, message={}", //$NON-NLS-1$
                    application, code, code.getMessage(arguments));
        } else {
            logger.info("failure: application={}, reason={}, message={}", //$NON-NLS-1$
                    application, code, code.getMessage(arguments),
                    cause);
        }
    }

    @Override
    public void onFailure(@Nonnull DiagnosticException exception) {
        Objects.requireNonNull(exception);
        logger.info("failure: application={}, reason={}, message={}", application, //$NON-NLS-1$
                application,
                exception.getDiagnosticCode(),
                exception.getDiagnosticCode().getMessage(exception.getArguments()),
                exception);
    }

    /**
     * no-op.
     */
    @Override
    public void close() {
        return;
    }

}
