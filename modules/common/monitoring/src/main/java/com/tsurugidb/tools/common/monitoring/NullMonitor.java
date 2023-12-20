package com.tsurugidb.tools.common.monitoring;

import java.util.List;

import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.value.Property;

/**
 * An implementation of {@link Monitor} that does nothing.
 */
public enum NullMonitor implements Monitor {

    /**
     * The singleton instance of {@link NullMonitor}.
     */
    INSTANCE,
    ;

    @Override
    public void onStart() {
        // does nothing.
        return;
    }

    @Override
    public void onData(String format, List<? extends Property> properties) {
        // does nothing.
        return;
    }

    @Override
    public void onSuccess() {
        // does nothing.
        return;
    }

    @Override
    public void onFailure(Throwable cause, DiagnosticCode code, List<?> arguments) {
        // does nothing.
        return;
    }

    @Override
    public void onFailure(DiagnosticException exception) throws MonitoringException {
        // does nothing.
        return;
    }

    @Override
    public void close() {
        // does nothing.
        return;
    }
}
