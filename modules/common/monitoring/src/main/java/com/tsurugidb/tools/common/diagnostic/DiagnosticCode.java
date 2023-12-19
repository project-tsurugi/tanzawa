package com.tsurugidb.tools.common.diagnostic;

import java.util.List;

/**
 * Represents a diagnostic type of operations.
 */
public interface DiagnosticCode {

    /**
     * Returns the tag of this diagnostic code.
     * @return the diagnostic tag
     */
    String getTag();

    /**
     * Returns the parameter count for the diagnostic message.
     * @return the parameter count
     */
    int getParameterCount();

    /**
     * Returns the diagnostic message.
     * @param parameters the message parameters
     * @return the message string
     * @throws IllegalArgumentException if the number of parameters is mismatched to {@link #getParameterCount()}
     */
    String getMessage(List<?> parameters);
}
