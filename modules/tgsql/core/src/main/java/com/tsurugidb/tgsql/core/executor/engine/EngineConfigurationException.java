/*
 * Copyright 2023-2025 Project Tsurugi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
