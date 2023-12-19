package com.tsurugidb.tools.common.connection;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;

/**
 * Exception occurred while establishing connection to Tsurugi server.
 */
public class ConnectionException extends DiagnosticException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     */
    public ConnectionException(@Nonnull ConnectionDiagnosticCode diagnosticCode, @Nonnull List<?> arguments) {
        this(diagnosticCode, arguments, null);
    }

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     * @param cause the original cause
     */
    public ConnectionException(
            @Nonnull ConnectionDiagnosticCode diagnosticCode, @Nonnull List<?> arguments,
            @Nullable Throwable cause) {
        super(diagnosticCode, arguments, cause);
    }

    @Override
    public ConnectionDiagnosticCode getDiagnosticCode() {
        return (ConnectionDiagnosticCode) super.getDiagnosticCode();
    }
}
