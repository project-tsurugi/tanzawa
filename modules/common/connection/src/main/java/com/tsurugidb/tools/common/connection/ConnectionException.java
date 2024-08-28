/*
 * Copyright 2023-2024 Project Tsurugi.
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
package com.tsurugidb.tools.common.connection;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;

/**
 * Exception occurred while establishing connection to Tsurugi server.
 */
public class ConnectionException extends DiagnosticException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     */
    public ConnectionException(@Nonnull ConnectionDiagnosticCode diagnosticCode, @Nonnull List<?> arguments) {
        this(diagnosticCode, arguments, null);
    }

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     * @param cause the original cause
     */
    public ConnectionException(
            @Nonnull ConnectionDiagnosticCode diagnosticCode, @Nonnull List<?> arguments,
            @Nullable Throwable cause) {
        super(diagnosticCode, arguments, cause);
    }

    @Override
    public ConnectionDiagnosticCode getDiagnosticCode() {
        return (ConnectionDiagnosticCode) super.getDiagnosticCode();
    }
}
