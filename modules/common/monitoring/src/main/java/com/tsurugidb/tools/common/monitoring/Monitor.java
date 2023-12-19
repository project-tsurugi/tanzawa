package com.tsurugidb.tools.common.monitoring;

import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.value.Property;

/**
 * An interface for operation monitors.
 */
public interface Monitor extends Closeable {

    /**
     * Invoked on the command was started.
     * @throws MonitoringException if error was occurred while processing the event
     */
    void onStart() throws MonitoringException;

    /**
     * Invoked on the operation was generated some informative data.
     * @param format the data format name
     * @param properties the properties of the generated data
     * @throws MonitoringException if error was occurred while processing the event
     */
    void onData(@Nonnull String format, @Nonnull List<? extends Property> properties) throws MonitoringException;

    /**
     * Invoked on the operation was generated some informative data.
     * @param format the data format name
     * @param properties the properties of the generated data
     * @throws MonitoringException if error was occurred while processing the event
     */
    default void onData(@Nonnull String format, @Nonnull Property... properties) throws MonitoringException {
        Objects.requireNonNull(format);
        Objects.requireNonNull(properties);
        onData(format, Arrays.asList(properties));
    }

    /**
     * Invoked on the operation was successfully finished.
     * @throws MonitoringException if error was occurred while processing the event
     */
    void onSuccess() throws MonitoringException;

    /**
     * Invoked on the operation was failed.
     * @param code the diagnostic code
     * @param arguments the diagnostic message arguments
     * @throws MonitoringException if error was occurred while processing the event
     */
    default void onFailure(@Nonnull DiagnosticCode code, @Nonnull List<?> arguments) throws MonitoringException {
        Objects.requireNonNull(code);
        Objects.requireNonNull(arguments);
        onFailure(null, code, arguments);
    }

    /**
     * Invoked on the operation was failed.
     * @param cause the failure cause, or {@code null} if it is not sure
     * @param code the diagnostic code
     * @param arguments the diagnostic message arguments
     * @throws MonitoringException if error was occurred while processing the event
     */
    void onFailure(@Nullable Throwable cause, @Nonnull DiagnosticCode code, @Nonnull List<?> arguments)
            throws MonitoringException;

    /**
     * Invoked on the operation was failed.
     * @param exception the occurred exception
     * @throws MonitoringException if error was occurred while processing the event
     */
    void onFailure(@Nonnull DiagnosticException exception) throws MonitoringException;
}
