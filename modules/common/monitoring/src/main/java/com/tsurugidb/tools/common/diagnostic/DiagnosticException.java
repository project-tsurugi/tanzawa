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
package com.tsurugidb.tools.common.diagnostic;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Exception with diagnostics.
 */
public class DiagnosticException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * The diagnostic code of this exception.
     */
    private final DiagnosticCode diagnosticCode;

    /**
     * The diagnostic message arguments.
     */
    private final List<?> arguments;

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     */
    public DiagnosticException(@Nonnull DiagnosticCode diagnosticCode, @Nonnull List<?> arguments) {
        this(diagnosticCode, arguments, null);
    }

    /**
     * Creates a new instance.
     * @param diagnosticCode the diagnostic code of this exception
     * @param arguments the diagnostic message arguments
     * @param cause the original cause
     */
    public DiagnosticException(
            @Nonnull DiagnosticCode diagnosticCode, @Nonnull List<?> arguments,
            @Nullable Throwable cause) {
        super(buildMessage(diagnosticCode, arguments), cause);
        this.diagnosticCode = diagnosticCode;
        this.arguments = List.copyOf(arguments);
    }

    private static String buildMessage(DiagnosticCode diagnosticCode, List<?> arguments) {
        Objects.requireNonNull(diagnosticCode);
        Objects.requireNonNull(arguments);
        return diagnosticCode.getMessage(arguments);
    }

    /**
     * Returns the diagnostic code.
     * @return the diagnostic code
     */
    public DiagnosticCode getDiagnosticCode() {
        return diagnosticCode;
    }

    /**
     * Returns the diagnostic message arguments.
     * @return the diagnostic message arguments
     */
    public List<?> getArguments() {
        return arguments;
    }
}
