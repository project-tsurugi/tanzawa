package com.tsurugidb.console.core.parser;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.console.core.model.Region;
import com.tsurugidb.console.core.model.ErroneousStatement.ErrorKind;

class ParseException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ErrorKind errorKind;

    private final Region occurrence;

    ParseException(
            @Nonnull ErrorKind errorKind,
            @Nonnull Region occurrence,
            @Nonnull String message) {
        super(message);
        Objects.requireNonNull(errorKind);
        Objects.requireNonNull(occurrence);
        this.errorKind = errorKind;
        this.occurrence = occurrence;
    }

    ErrorKind getErrorKind() {
        return errorKind;
    }

    Region getOccurrence() {
        return occurrence;
    }
}
