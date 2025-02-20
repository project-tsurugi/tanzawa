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
package com.tsurugidb.tools.tgdump.core.engine;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;
import com.tsurugidb.tools.common.monitoring.Monitor;

/**
 * Diagnostic codes for the monitoring operation.
 * @see Monitor
 */
public enum DumpDiagnosticCode implements DiagnosticCode {

    /**
     * {@code unknown} - unknown error.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the message </li>
     * </ul>
     */
    UNKNOWN("unknown", "unknown exception was occurred: {0}"), //$NON-NLS-1$, //$NON-NLS-2$


    /**
     * {@code table_not_found} - the source table is not found on the database.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the table name </li>
     * </ul>
     */
    TABLE_NOT_FOUND("table_not_found", "source table is not found on the database: {0}"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code begin_failure} - transaction cannot be started.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> Not available. </li>
     * </ul>
     */
    BEGIN_FAILURE("begin_failure", "transaction cannot be started"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code prepare_failure} - cannot create prepare statement for dump operation.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the table name </li>
     * <li> {@code [1]} - SQL command </li>
     * </ul>
     */
    PREPARE_FAILURE("prepare_failure", "command preparation was failed: {0} ({1})"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code operation_failure} - individual dump operations were failed.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the table name </li>
     * <li> {@code [1]} - the destination path </li>
     * </ul>
     */
    OPERATION_FAILURE("operation_failure", "dump operation was failed: {0} ({1})"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code commit_failure} - commit operation was failed.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> Not available. </li>
     * </ul>
     */
    COMMIT_FAILURE("commit_failure", "transaction was aborted}"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code io} - the dump operation was failed by I/O error.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the I/O error message </li>
     * </ul>
     */
    IO_ERROR("io", "dump operation was failed by I/O error: {0}"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code server} - the dump operation was failed by server-side error.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the error message from the server </li>
     * </ul>
     */
    SERVER_ERROR("server", "dump operation was failed by server: {0}"), //$NON-NLS-1$ //$NON-NLS-2$
    ;

    private final String tag;

    private final String format;

    private final int parameterCount;

    DumpDiagnosticCode(String tag, String format) {
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
