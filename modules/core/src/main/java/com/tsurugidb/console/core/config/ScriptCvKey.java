package com.tsurugidb.console.core.config;

import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.console.core.exception.ScriptMessageException;

/**
 * client variable key.
 *
 * @param <T> variable type
 * @see ScriptClientVariableMap
 */
public abstract class ScriptCvKey<T> {

    /** select.maxlines */
    public static final ScriptCvKeyInt SELECT_MAX_LINES = new ScriptCvKeyInt("select.maxlines"); //$NON-NLS-1$

    //

    /**
     * client variable key for String.
     */
    public static class ScriptCvKeyString extends ScriptCvKey<String> {

        /**
         * Creates a new instance.
         *
         * @param name variable name
         */
        public ScriptCvKeyString(String name) {
            super(name);
        }

        @Override
        public String convertValue(String s) {
            return s;
        }
    }

    /**
     * client variable key for Integer.
     */
    public static class ScriptCvKeyInt extends ScriptCvKey<Integer> {

        /**
         * Creates a new instance.
         *
         * @param name variable name
         */
        public ScriptCvKeyInt(String name) {
            super(name);
        }

        @Override
        public Integer convertValue(String s) {
            try {
                return Integer.valueOf(s.trim());
            } catch (NumberFormatException e) {
                throw new ScriptMessageException(MessageFormat.format("not integer. value={0}", s), e);
            }
        }
    }

    private static final Map<String, ScriptCvKey<?>> KEY_MAP = new HashMap<>();
    static {
        for (var field : ScriptCvKey.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && ScriptCvKey.class.isAssignableFrom(field.getType())) {
                try {
                    var key = ScriptCvKey.class.cast(field.get(null));
                    KEY_MAP.put(key.toString(), key);
                } catch (Exception e) {
                    throw new InternalError(e);
                }
            }
        }
    }

    /**
     * find key.
     *
     * @param name variable name
     * @return key
     */
    @Nullable
    public static ScriptCvKey<?> find(String name) {
        return KEY_MAP.get(name);
    }

    //

    private final String name;

    protected ScriptCvKey(String name) {
        this.name = name;
    }

    /**
     * convert value.
     *
     * @param s source value
     * @return converted value
     */
    public abstract T convertValue(@Nonnull String s);

    /**
     * cast value.
     *
     * @param obj value
     * @return casted value
     */
    @SuppressWarnings("unchecked")
    public T castValue(Object obj) {
        return (T) obj;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
