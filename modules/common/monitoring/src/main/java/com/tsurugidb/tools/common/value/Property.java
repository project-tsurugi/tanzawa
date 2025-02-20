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
package com.tsurugidb.tools.common.value;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a key-value pair.
 */
public class Property {

    private final String name;

    private final Value value;

    /**
     * Creates a new instance.
     * @param name the property name
     * @param value the property value
     */
    public Property(@Nonnull String name, @Nonnull Value value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        this.name = name;
        this.value = value;
    }

    /**
     * Creates a new instance.
     * @param name the property name
     * @param value the property value as object
     * @return the created instance
     * @throws IllegalArgumentException if it contains a value that not suitable for the property value
     */
    public static Property of(@Nonnull String name, @Nullable Object value) {
        Objects.requireNonNull(name);
        return new Property(name, Value.fromObject(value));
    }

    /**
     * Returns the property name.
     * @return the property name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the property value.
     * @return the property value
     */
    public Value getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Property other = (Property) obj;
        return Objects.equals(name, other.name) && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", name, value); //$NON-NLS-1$
    }
}
