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
