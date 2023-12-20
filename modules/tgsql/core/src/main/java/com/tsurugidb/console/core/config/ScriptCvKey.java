package com.tsurugidb.console.core.config;

import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.console.core.exception.ScriptMessageException;
import com.tsurugidb.console.core.executor.explain.DotOutputHandler;

/**
 * client variable key.
 *
 * @param <T> variable type
 * @see ScriptClientVariableMap
 */
public abstract class ScriptCvKey<T> {

    /** select.maxlines . */
    public static final ScriptCvKeyInt SELECT_MAX_LINES = new ScriptCvKeyInt("select.maxlines"); //$NON-NLS-1$
    /** sql.timing . */
    public static final ScriptCvKeyBoolean SQL_TIMING = new ScriptCvKeyBoolean("sql.timing"); //$NON-NLS-1$
    /** sql.timing . */
    public static final ScriptCvKeyBoolean AUTO_COMMIT_TX_STARTED_IMPLICITLY = new ScriptCvKeyBoolean("auto-commit.when-transaction-started-implicitly"); //$NON-NLS-1$

    /** dot.verbose . */
    public static final ScriptCvKeyBoolean DOT_VERBOSE = new ScriptCvKeyBoolean(DotOutputHandler.KEY_VERBOSE);
    /** dot.output . */
    public static final ScriptCvKeyString DOT_OUTPUT = new ScriptCvKeyString(DotOutputHandler.KEY_OUTPUT);
    /** dot.executable . */
    public static final ScriptCvKeyString DOT_EXECUTABLE = new ScriptCvKeyString(DotOutputHandler.KEY_EXECUTABLE);
    /** dot.graph.* . */
    public static final ScriptCvKeyString DOT_GRAPH_PREFIX = new ScriptCvKeyString(DotOutputHandler.KEY_GRAPH_PREFIX);
    /** dot.node.* . */
    public static final ScriptCvKeyString DOT_NODE_PREFIX = new ScriptCvKeyString(DotOutputHandler.KEY_NODE_PREFIX);
    /** dot.edge.* . */
    public static final ScriptCvKeyString DOT_EDGE_PREFIX = new ScriptCvKeyString(DotOutputHandler.KEY_EDGE_PREFIX);

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
        public Integer convertValue(@Nonnull String s) {
            try {
                return Integer.valueOf(s.trim());
            } catch (NumberFormatException e) {
                throw new ScriptMessageException(MessageFormat.format("not integer. key={0}, value={1}", name, s), e);
            }
        }
    }

    /**
     * client variable key for Boolean.
     */
    public static class ScriptCvKeyBoolean extends ScriptCvKey<Boolean> {

        /**
         * Creates a new instance.
         *
         * @param name variable name
         */
        public ScriptCvKeyBoolean(String name) {
            super(name);
        }

        @Override
        public Boolean convertValue(@Nonnull String s) {
            String t = s.trim().toLowerCase(Locale.ENGLISH);
            if (t.isEmpty()) {
                return Boolean.FALSE;
            }
            if ("true".startsWith(t)) { //$NON-NLS-1$
                return Boolean.TRUE;
            }
            switch (t) {
            case "on":
                return true;
            case "off":
                return false;
            default:
                break;
            }
            try {
                return Double.parseDouble(t) != 0;
            } catch (Exception ignore) {
                // ignore
            }
            return false;
        }
    }

    /**
     * client variable key for Color.
     */
    public static class ScriptCvKeyColor extends ScriptCvKey<ScriptColor> {

        /**
         * Creates a new instance.
         *
         * @param name variable name
         */
        public ScriptCvKeyColor(String name) {
            super(name);
        }

        @Override
        public ScriptColor convertValue(@Nonnull String s) {
            return ScriptColor.parse(s);
        }
    }

    private static final Map<String, ScriptCvKey<?>> KEY_MAP = new ConcurrentHashMap<>();
    static {
        registerKey(ScriptCvKey.class);
    }

    /**
     * register key.
     *
     * @param clazz class with key defined
     */
    public static void registerKey(Class<?> clazz) {
        for (var field : clazz.getFields()) {
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
     * get key names.
     *
     * @return key names
     */
    public static Set<String> getKeyNames() {
        return KEY_MAP.keySet();
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

    /** variable name. */
    protected final String name;

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
