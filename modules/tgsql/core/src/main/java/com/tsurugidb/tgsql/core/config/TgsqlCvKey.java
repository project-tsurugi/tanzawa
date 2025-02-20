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
package com.tsurugidb.tgsql.core.config;

import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tgsql.core.exception.TgsqlMessageException;
import com.tsurugidb.tgsql.core.executor.explain.DotOutputHandler;

/**
 * client variable key.
 *
 * @param <T> variable type
 * @see TgsqlClientVariableMap
 */
public abstract class TgsqlCvKey<T> {

    /** display.implicit . */
    public static final TgsqlCvKeyBoolean DISPLAY_IMPLICIT = new TgsqlCvKeyBoolean("display.implicit"); //$NON-NLS-1$
    /** display.succeed . */
    public static final TgsqlCvKeyBoolean DISPLAY_SUCCEED = new TgsqlCvKeyBoolean("display.succeed"); //$NON-NLS-1$

    /** implicit-transaction.label.suffix-time . */
    public static final TgsqlCvKeyDateTimeFormat IMPLICIT_TX_LABEL_SUFFIX_TIME = new TgsqlCvKeyDateTimeFormat("implicit-transaction.label.suffix-time"); //$NON-NLS-1$
    /** implicit-transaction.auto-commit . */
    public static final TgsqlCvKeyBoolean IMPLICIT_TX_AUTO_COMMIT = new TgsqlCvKeyBoolean("implicit-transaction.auto-commit"); //$NON-NLS-1$

    /** transaction.label.suffix-time . */
    public static final TgsqlCvKeyDateTimeFormat TX_LABEL_SUFFIX_TIME = new TgsqlCvKeyDateTimeFormat("transaction.label.suffix-time"); //$NON-NLS-1$

    /** select.maxlines . */
    public static final TgsqlCvKeyInt SELECT_MAX_LINES = new TgsqlCvKeyInt("select.maxlines"); //$NON-NLS-1$
    /** sql.timing . */
    public static final TgsqlCvKeyBoolean SQL_TIMING = new TgsqlCvKeyBoolean("sql.timing"); //$NON-NLS-1$

    // @see DotOutputHandler#extendOptions(Map, TgsqlClientVariableMap)
    /** dot.verbose . */
    public static final TgsqlCvKeyBoolean DOT_VERBOSE = new TgsqlCvKeyBoolean(DotOutputHandler.KEY_VERBOSE);
    /** dot.output . */
    public static final TgsqlCvKeyString DOT_OUTPUT = new TgsqlCvKeyString(DotOutputHandler.KEY_OUTPUT);
    /** dot.executable . */
    public static final TgsqlCvKeyString DOT_EXECUTABLE = new TgsqlCvKeyString(DotOutputHandler.KEY_EXECUTABLE);
    /** dot.graph.* . */
    public static final TgsqlCvKeyString DOT_GRAPH_PREFIX = new TgsqlCvKeyString(DotOutputHandler.KEY_GRAPH_PREFIX);
    /** dot.node.* . */
    public static final TgsqlCvKeyString DOT_NODE_PREFIX = new TgsqlCvKeyString(DotOutputHandler.KEY_NODE_PREFIX);
    /** dot.edge.* . */
    public static final TgsqlCvKeyString DOT_EDGE_PREFIX = new TgsqlCvKeyString(DotOutputHandler.KEY_EDGE_PREFIX);

    //

    /**
     * client variable key for String.
     */
    public static class TgsqlCvKeyString extends TgsqlCvKey<String> {

        /**
         * Creates a new instance.
         *
         * @param name variable name
         */
        public TgsqlCvKeyString(String name) {
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
    public static class TgsqlCvKeyInt extends TgsqlCvKey<Integer> {

        /**
         * Creates a new instance.
         *
         * @param name variable name
         */
        public TgsqlCvKeyInt(String name) {
            super(name);
        }

        @Override
        public Integer convertValue(@Nonnull String s) {
            try {
                return Integer.valueOf(s.trim());
            } catch (NumberFormatException e) {
                throw new TgsqlMessageException(MessageFormat.format("not integer. key={0}, value={1}", name, s), e);
            }
        }
    }

    /**
     * client variable key for Boolean.
     */
    public static class TgsqlCvKeyBoolean extends TgsqlCvKey<Boolean> {

        /**
         * Creates a new instance.
         *
         * @param name variable name
         */
        public TgsqlCvKeyBoolean(String name) {
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
    public static class TgsqlCvKeyColor extends TgsqlCvKey<TgsqlColor> {

        /**
         * Creates a new instance.
         *
         * @param name variable name
         */
        public TgsqlCvKeyColor(String name) {
            super(name);
        }

        @Override
        public TgsqlColor convertValue(@Nonnull String s) {
            return TgsqlColor.parse(s);
        }
    }

    /**
     * client variable key for Prompt.
     */
    public static class TgsqlCvKeyPrompt extends TgsqlCvKey<TgsqlPrompt> {

        /**
         * Creates a new instance.
         *
         * @param name variable name
         */
        public TgsqlCvKeyPrompt(String name) {
            super(name);
        }

        @Override
        public TgsqlPrompt convertValue(String s) {
            return TgsqlPrompt.create(s);
        }
    }

    /**
     * client variable key for DateTimeFormat.
     */
    public static class TgsqlCvKeyDateTimeFormat extends TgsqlCvKey<TgsqlDateTimeFormat> {

        /**
         * Creates a new instance.
         *
         * @param name variable name
         */
        public TgsqlCvKeyDateTimeFormat(String name) {
            super(name);
        }

        @Override
        public TgsqlDateTimeFormat convertValue(@Nonnull String s) {
            try {
                return TgsqlDateTimeFormat.create(s);
            } catch (Exception e) {
                throw new TgsqlMessageException(MessageFormat.format("dateTime format error. key={0}, value={1}, cause={2}", name, s, e.getMessage()), e);
            }
        }
    }

    //

    private static final Map<String, TgsqlCvKey<?>> KEY_MAP = new ConcurrentHashMap<>();
    static {
        registerKey(TgsqlCvKey.class);
    }

    /**
     * register key.
     *
     * @param clazz class with key defined
     */
    public static void registerKey(Class<?> clazz) {
        for (var field : clazz.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && TgsqlCvKey.class.isAssignableFrom(field.getType())) {
                try {
                    var key = TgsqlCvKey.class.cast(field.get(null));
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
    public static @Nullable TgsqlCvKey<?> find(String name) {
        return KEY_MAP.get(name);
    }

    //

    /** variable name. */
    protected final String name;

    protected TgsqlCvKey(String name) {
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
