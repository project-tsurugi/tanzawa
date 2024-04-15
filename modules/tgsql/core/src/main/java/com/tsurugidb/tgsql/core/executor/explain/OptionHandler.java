package com.tsurugidb.tgsql.core.executor.explain;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * Handles key-value options.
 */
public interface OptionHandler {

    /**
     * Returns whether or not this handler handles the specified option.
     * @param key the option name (ignoring case)
     * @return {@code true} if this handled the option; otherwise {@code false}
     */
    default boolean isHandled(@Nonnull String key) {
        Objects.requireNonNull(key);
        return false;
    }
}
