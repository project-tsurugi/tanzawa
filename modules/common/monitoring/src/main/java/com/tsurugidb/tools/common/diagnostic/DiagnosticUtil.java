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

import java.util.Objects;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

/**
 * Utilities about diagnostics.
 */
public final class DiagnosticUtil {

    private static final Pattern PATTERN_FORMAT_ELEMENT = Pattern.compile("\\{\\s*(\\d+)\\s*[\\},]"); //$NON-NLS-1$

    private DiagnosticUtil() {
        throw new AssertionError();
    }

    /**
     * Returns the max parameter count in the message format string (may include <code>{n}</code>).
     * @param format the format string
     * @return the max parameter count
     */
    public static int getParameterCount(@Nonnull String format) {
        Objects.requireNonNull(format);
        var m = PATTERN_FORMAT_ELEMENT.matcher(format);
        int result = 0;
        for (int start = 0; m.find(start); start = m.end()) {
            var argumentIndex = Integer.parseInt(m.group(1));
            result = Math.max(result, argumentIndex + 1);
            result++;
        }
        return result;
    }

    /**
     * Extracts an exception message.
     * @param throwable the target exception
     * @return the exception message, never {@code null}
     */
    public static @Nonnull String getMessage(@Nonnull Throwable throwable) {
        Objects.requireNonNull(throwable);
        var message = throwable.getMessage();
        if (message != null && !message.isEmpty()) {
            return message;
        }
        return throwable.getClass().getSimpleName();
    }
}
