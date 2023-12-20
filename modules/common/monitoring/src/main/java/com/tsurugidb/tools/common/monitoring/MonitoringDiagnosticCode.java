package com.tsurugidb.tools.common.monitoring;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;

/**
 * Diagnostic codes for the monitoring operation.
 * @see Monitor
 */
public enum MonitoringDiagnosticCode implements DiagnosticCode {

    /**
     * {@code monitor_output} - cannot output monitoring result.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - output resource </li>
     * </ul>
     */
    OUTPUT_ERROR(
            "monitor_output", //$NON-NLS-1$
            "cannot output monitoring result: {0}"), //$NON-NLS-1$

    ;

    private final String tag;

    private final String format;

    private final int parameterCount;

    MonitoringDiagnosticCode(String tag, String format) {
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
