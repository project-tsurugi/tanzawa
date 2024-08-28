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
package com.tsurugidb.tools.tgdump.profile;

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
public enum ProfileDiagnosticCode implements DiagnosticCode {

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
     * {@code profile_not_found} - the source file is not found.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the profile location </li>
     * </ul>
     */
    PROFILE_NOT_FOUND("profile_not_found", "dump profile file is not found: {0}"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code profile_not_registered} - the profile name is not found in the dump profile bundle.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the profile name </li>
     * <li> {@code [1]} - the available profile names </li>
     * </ul>
     */
    PROFILE_NOT_REGISTERED("profile_not_registered", "dump profile is not registered: {0} (available: {1})"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code profile_invalid} - the source file is ill-formed.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the profile location </li>
     * <li> {@code [1]} - the causal message </li>
     * </ul>
     */
    PROFILE_INVALID("profile_invalid", "dump profile file is not valid: {0} ({1})"), //$NON-NLS-1$ //$NON-NLS-2$

    /**
     * {@code profile_unsupported} - the unsupported profile format.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the profile location </li>
     * <li> {@code [1]} - the file format version </li>
     * <li> {@code [2]} - expected format version </li>
     * </ul>
     */
    PROFILE_UNSUPPORTED("profile_unsupported", //$NON-NLS-1$
            "dump profile file is unsupported format version: {1} ({0}, expected: {2})"), //$NON-NLS-1$

    /**
     * {@code io} - loading profile was failed by I/O error.
     *
     * <p>
     * Parameters:
     * </p>
     * <ul>
     * <li> {@code [0]} - the profile or bundle index location </li>
     * <li> {@code [1]} - the I/O error message </li>
     * </ul>
     */
    IO_ERROR("io", "loading dump profile was failed by I/O error: {0} ({1})"), //$NON-NLS-1$
    ;

    private final String tag;

    private final String format;

    private final int parameterCount;

    ProfileDiagnosticCode(String tag, String format) {
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
