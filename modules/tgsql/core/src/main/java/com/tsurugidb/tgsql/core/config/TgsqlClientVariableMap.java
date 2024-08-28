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
package com.tsurugidb.tgsql.core.config;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * client variable map.
 */
public class TgsqlClientVariableMap {

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
        var cvKey = TgsqlCvKey.find(key);
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
    public <T> T put(@Nonnull TgsqlCvKey<T> key, @Nullable String value) {
        T converted = (value != null) ? key.convertValue(value) : null;
        if (converted == null) {
            variableMap.remove(key.toString());
            return null;
        }
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
    public <T> T put(@Nonnull TgsqlCvKey<T> key, @Nullable T value) {
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
    public <T> T get(@Nonnull TgsqlCvKey<T> key) {
        var obj = variableMap.get(key.toString());
        return key.castValue(obj);
    }

    /**
     * Returns the value to which the specified key is mapped,or null if this map contains no mapping for the key.
     *
     * @param <T>          variable type
     * @param key          the key whose associated value is to be returned
     * @param defaultValue default value
     * @return the value to which the specified key is mapped, or null if this map contains no mapping for the key
     */
    @Nullable
    public <T> T get(@Nonnull TgsqlCvKey<T> key, T defaultValue) {
        var value = get(key);
        return (value != null) ? value : defaultValue;
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
