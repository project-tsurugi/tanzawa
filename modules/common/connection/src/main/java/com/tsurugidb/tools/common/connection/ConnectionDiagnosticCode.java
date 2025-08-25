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
package com.tsurugidb.tools.common.connection;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;

/**
 * Diagnostic codes for the connections to Tsurugi.
 * @see ConnectionProvider
 */
public enum ConnectionDiagnosticCode implements DiagnosticCode {

    /**
     * {@code credential_error} - retrieving credential was failed.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the error message </li>
     * <li> {@code [1]} - the credential type </li>
     * </ul>
     */
    CREDENTIAL_ERROR(
            "credential_error", //$NON-NLS-1$
            "failed to retrieve credential: {1} ({0})"), //$NON-NLS-1$

    /**
     * {@code authentication_failure} - authentication was failed.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the endpoint URI </li>
     * </ul>
     */
    AUTHENTICATION_FAILURE(
            "authentication_failure", //$NON-NLS-1$
            "credential was rejected: {0}"), //$NON-NLS-1$

    /**
     * {@code connection_timeout} - establishing connection to Tsurugi was timed out.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the endpoint URI </li>
     * <li> {@code [1]} - the timeout duration in millisecond </li>
     * </ul>
     */
    TIMEOUT(
            "connection_timeout", //$NON-NLS-1$
            "connection to Tsurugi was timed out: {0} ({1}ms)"), //$NON-NLS-1$

    /**
     * {@code connection_failure} - connection was rejected.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the endpoint URI </li>
     * <li> {@code [1]} - an error message from the server </li>
     * </ul>
     */
    FAILURE(
            "connection_failure", //$NON-NLS-1$
            "connection to Tsurugi was rejected: {0} ({1})"), //$NON-NLS-1$

    /**
     * {@code io} - connection was failed by I/O error.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the I/O error message </li>
     * </ul>
     */
    IO_ERROR("io", "connection to Tsurugi was failed by I/O error: {0}"), //$NON-NLS-1$ //$NON-NLS-2$

    ;

    private final String tag;

    private final String format;

    private final int parameterCount;

    ConnectionDiagnosticCode(String tag, String format) {
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
