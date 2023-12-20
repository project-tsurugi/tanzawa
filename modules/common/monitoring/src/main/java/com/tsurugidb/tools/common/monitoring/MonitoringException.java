package com.tsurugidb.tools.common.monitoring;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;

/**
 * Exception occurred on {@link Monitor} operations.
 */
public class MonitoringException extends DiagnosticException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     */
    public MonitoringException(@Nonnull MonitoringDiagnosticCode diagnosticCode, @Nonnull List<?> arguments) {
        this(diagnosticCode, arguments, null);
    }

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     * @param cause the original cause
     */
    public MonitoringException(
            @Nonnull MonitoringDiagnosticCode diagnosticCode, @Nonnull List<?> arguments,
            @Nullable Throwable cause) {
        super(diagnosticCode, arguments, cause);
    }

    @Override
    public MonitoringDiagnosticCode getDiagnosticCode() {
        return (MonitoringDiagnosticCode) super.getDiagnosticCode();
    }
}
