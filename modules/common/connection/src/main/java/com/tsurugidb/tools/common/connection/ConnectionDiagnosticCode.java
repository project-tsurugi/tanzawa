package com.tsurugidb.tools.common.connection;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;

/**
 * Diagnostic codes for the connections to Tsurugi.
 * @see ConnectionProvider
 */
public enum ConnectionDiagnosticCode implements DiagnosticCode {

    /**
     * {@code authentication_failure} - authentication was failed.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the endpoint URI </li>
     * </ul>
     */
    AUTHENTICATION_FAILURE(
            "authentication_failure", //$NON-NLS-1$
            "credential was rejected: {0}"), //$NON-NLS-1$

    /**
     * {@code connection_timeout} - establishing connection to Tsurugi was timed out.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the endpoint URI </li>
     * <li> {@code [1]} - the timeout duration in millisecond </li>
     * </ul>
     */
    TIMEOUT(
            "connection_timeout", //$NON-NLS-1$
            "connection to Tsurugi was timed out: {0} ({1}ms)"), //$NON-NLS-1$

    /**
     * {@code connection_failure} - connection was rejected.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the endpoint URI </li>
     * <li> {@code [1]} - an error message from the server </li>
     * </ul>
     */
    FAILURE(
            "connection_failure", //$NON-NLS-1$
            "connection to Tsurugi was rejected: {0} ({1})"), //$NON-NLS-1$

    /**
     * {@code io} - connection was failed by I/O error.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the I/O error message </li>
     * </ul>
     */
    IO_ERROR("io", "connection to Tsurugi was failed by I/O error: {0}"), //$NON-NLS-1$ //$NON-NLS-2$

    ;

    private final String tag;

    private final String format;

    private final int parameterCount;

    ConnectionDiagnosticCode(String tag, String format) {
        this.tag = tag;
        this.format = format;
        this.parameterCount = DiagnosticUtil.getParameterCount(format);
    }

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public int getParameterCount() {
        return parameterCount;
    }

    @Override
    public String getMessage(@Nonnull List<?> parameters) {
        Objects.requireNonNull(parameters);
        return MessageFormat.format(format, parameters.toArray());
    }

}
