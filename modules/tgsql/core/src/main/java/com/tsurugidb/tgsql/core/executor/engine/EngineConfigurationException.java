package com.tsurugidb.tgsql.core.executor.engine;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tgsql.core.model.ErroneousStatement;
import com.tsurugidb.tgsql.core.model.Region;
import com.tsurugidb.tgsql.core.model.ErroneousStatement.ErrorKind;

/**
 * Errors in configuring {@link Engine}, should be interpreted as an {@link ErroneousStatement}.
 */
public class EngineConfigurationException extends EngineException {

    private static final long serialVersionUID = 1L;

    private final ErrorKind errorKind;

    private final Region occurrence;

    /**
     * Creates a new instance.
     * @param errorKind the error kind
     * @param occurrence the error occurrence region
     * @param message the error message
     */
    public EngineConfigurationException(
            @Nonnull ErroneousStatement.ErrorKind errorKind,
            @Nonnull Region occurrence,
            @Nonnull String message) {
        this(errorKind, occurrence, message, null);
    }

    /**
     * Creates a new instance.
     * @param errorKind the error kind
     * @param occurrence the error occurrence region
     * @param message the error message
     * @param cause the original cause
     */
    public EngineConfigurationException(
            @Nonnull ErroneousStatement.ErrorKind errorKind,
            @Nonnull Region occurrence,
            @Nonnull String message,
            @Nullable Throwable cause) {
        super(message, cause);
        Objects.requireNonNull(errorKind);
        Objects.requireNonNull(occurrence);
        this.errorKind = errorKind;
        this.occurrence = occurrence;
    }

    /**
     * Returns the error kind.
     * @return the error kind
     */
    public ErrorKind getErrorKind() {
        return errorKind;
    }

    /**
     * Returns the error occurrence region.
     * @return the error occurrence region
     */
    public Region getOccurrence() {
        return occurrence;
    }
}
