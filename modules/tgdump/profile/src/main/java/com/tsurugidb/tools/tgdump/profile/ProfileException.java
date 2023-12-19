package com.tsurugidb.tools.tgdump.profile;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;

/**
 * Exception occurred on loading profiles.
 */
public class ProfileException extends DiagnosticException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     */
    public ProfileException(@Nonnull ProfileDiagnosticCode diagnosticCode, @Nonnull List<?> arguments) {
        this(diagnosticCode, arguments, null);
    }

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     * @param cause the original cause
     */
    public ProfileException(
            @Nonnull ProfileDiagnosticCode diagnosticCode, @Nonnull List<?> arguments,
            @Nullable Throwable cause) {
        super(diagnosticCode, arguments, cause);
    }

    @Override
    public ProfileDiagnosticCode getDiagnosticCode() {
        return (ProfileDiagnosticCode) super.getDiagnosticCode();
    }
}
