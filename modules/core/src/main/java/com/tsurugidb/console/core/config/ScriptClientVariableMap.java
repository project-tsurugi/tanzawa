package com.tsurugidb.console.core.config;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * client variable map.
 */
public class ScriptClientVariableMap {

    private final Map<String, Object> variableMap = new TreeMap<>();

    /**
     * Copies all of the mappings from the specified map to this map.
     *
     * @param map mappings to be stored in this map
     */
    public void putAll(@Nonnull Map<String, String> map) {
        for (var entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return converted value
     */
    public Object put(@Nonnull String key, @Nullable String value) {
        var cvKey = ScriptCvKey.find(key);
        if (cvKey != null) {
            return put(cvKey, value);
        } else {
            variableMap.put(key, value);
            return value;
        }
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @param <T>   variable type
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return converted value
     */
    public <T> T put(@Nonnull ScriptCvKey<T> key, @Nullable String value) {
        T converted = (value != null) ? key.convertValue(value) : null;
        return put(key, converted);
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @param <T>   variable type
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return value
     */
    public <T> T put(@Nonnull ScriptCvKey<T> key, @Nullable T value) {
        variableMap.put(key.toString(), value);
        return value;
    }

    /**
     * Returns the value to which the specified key is mapped,or null if this map contains no mapping for the key.
     *
     * @param <T> variable type
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or null if this map contains no mapping for the key
     */
    @Nullable
    public <T> T get(@Nonnull ScriptCvKey<T> key) {
        var obj = variableMap.get(key.toString());
        return key.castValue(obj);
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return variableMap.size();
    }

    /**
     * Returns a Set view of the mappings contained in this map.
     *
     * @return a set view of the mappings contained in this map
     */
    public Set<Entry<String, Object>> entrySet() {
        return variableMap.entrySet();
    }
}
