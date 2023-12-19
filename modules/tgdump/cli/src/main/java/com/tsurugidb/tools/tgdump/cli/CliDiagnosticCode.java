package com.tsurugidb.tools.tgdump.cli;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;
import com.tsurugidb.tools.common.monitoring.Monitor;

/**
 * Diagnostic codes for the monitoring operation.
 * @see Monitor
 */
public enum CliDiagnosticCode implements DiagnosticCode {

    /**
     * {@code unknown} - unknown error.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the message </li>
     * </ul>
     */
    UNKNOWN("unknown", "unknown exception was occurred: {0}"), //$NON-NLS-1$, //$NON-NLS-2$

    /**
     * {@code io} - dump operation was failed by I/O error.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the I/O error message </li>
     * </ul>
     */
    IO_ERROR("io", "dump operation was failed by I/O error: {0}"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code interrupted} - operation was interrupted.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> not available </li>
     * </ul>
     */
    INTERRUPTED("interrupted", "operation was interrupted"), //$NON-NLS-1$, //$NON-NLS-2$

    /**
     * {@code internal} - internal error.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the message </li>
     * </ul>
     */
    INTERNAL("internal", "internal error was occurred: {0}"), //$NON-NLS-1$, //$NON-NLS-2$

    /**
     * {@code server} - the command was failed by server-side error.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the error message from the server </li>
     * </ul>
     */
    SERVER_ERROR("server", "server-side error was occurred: {0}"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code invalid_parameter} - command parameters were not valid.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the message </li>
     * </ul>
     */
    INVALID_PARAMETER("invalid_parameter", "command parameter was not valid: {0}"), //$NON-NLS-1$, //$NON-NLS-2$

    /**
     * {@code destination_exists} - dump output destination directory is already exists and not empty.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the destination path </li>
     * </ul>
     */
    DESTINATION_EXISTS("destination_exists", "dump output destination directory is already exists and not empty: {0}"), //$NON-NLS-1$, //$NON-NLS-2$

    /**
     * {@code destination_failure} - failed to create dump output destination directory.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the destination path </li>
     * </ul>
     */
    DESTINATION_FAILURE("destination_failure", "failed to create dump output destination directory: {0}"), //$NON-NLS-1$, //$NON-NLS-2$
    ;

    private final String tag;

    private final String format;

    private final int parameterCount;

    CliDiagnosticCode(String tag, String format) {
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
