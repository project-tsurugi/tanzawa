package com.tsurugidb.tools.tgdump.cli;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;

/**
 * Exception occurred on dump command preparation.
 */
public class CliException extends DiagnosticException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     */
    public CliException(@Nonnull CliDiagnosticCode diagnosticCode, @Nonnull List<?> arguments) {
        this(diagnosticCode, arguments, null);
    }

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     * @param cause the original cause
     */
    public CliException(
            @Nonnull CliDiagnosticCode diagnosticCode, @Nonnull List<?> arguments,
            @Nullable Throwable cause) {
        super(diagnosticCode, arguments, cause);
    }

    @Override
    public CliDiagnosticCode getDiagnosticCode() {
        return (CliDiagnosticCode) super.getDiagnosticCode();
    }
}
