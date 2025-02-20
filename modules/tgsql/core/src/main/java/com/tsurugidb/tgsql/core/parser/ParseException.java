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
package com.tsurugidb.tgsql.core.parser;

import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.tgsql.core.model.Region;
import com.tsurugidb.tgsql.core.model.ErroneousStatement.ErrorKind;

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
