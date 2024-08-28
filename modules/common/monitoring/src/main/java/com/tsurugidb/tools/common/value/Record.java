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
package com.tsurugidb.tools.common.value;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * Represents a list of {@link Property properties}.
 */
public class Record implements Iterable<Property> {

    private final List<Property> properties;

    /**
     * Creates a new empty instance.
     */
    public Record() {
        this.properties = List.of();
    }

    /**
     * Creates a new instance.
     * @param properties the record properties
     */
    public Record(@Nonnull List<? extends Property> properties) {
        Objects.requireNonNull(properties);
        this.properties = List.copyOf(properties);
    }

    /**
     * Creates a new instance.
     * @param properties the record properties
     * @return the created object
     */
    public static Record of(@Nonnull Property... properties) {
        Objects.requireNonNull(properties);
        return new Record(Arrays.asList(properties));
    }

    /**
     * Returns the properties.
     * @return the properties
     */
    public List<Property> getProperties() {
        return properties;
    }

    /**
     * Returns an iterator over the properties.
     * @return the properties
     */
    @Override
    public Iterator<Property> iterator() {
        return properties.iterator();
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties);
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
        Record other = (Record) obj;
        return Objects.equals(properties, other.properties);
    }

    @Override
    public String toString() {
        return properties.stream()
                .map(Property::toString)
                .collect(Collectors.joining(", ", "{", "}")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
