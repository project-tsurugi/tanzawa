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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * Represents an array of {@link Value values}.
 */
public class Array implements Iterable<Value> {

    private final List<Value> elements;

    /**
     * Creates a new empty instance.
     */
    public Array() {
        this.elements = List.of();
    }

    /**
     * Creates a new instance.
     * @param elements the array elements
     */
    public Array(@Nonnull List<? extends Value> elements) {
        Objects.requireNonNull(elements);
        this.elements = List.copyOf(elements);
    }

    /**
     * Convert a list of objects into the corresponding {@link Array} object.
     * @param elements the target list
     * @return the converted object
     * @throws IllegalArgumentException if it contains a value that not suitable for the array element
     */
    public static Array fromList(@Nonnull List<?> elements) {
        Objects.requireNonNull(elements);
        var converted = new ArrayList<Value>(elements.size());
        for (var element : elements) {
            converted.add(Value.fromObject(element));
        }
        return new Array(converted);
    }

    /**
     * Convert an array of objects into the corresponding {@link Array} object.
     * @param elements the target array
     * @return the converted object
     * @throws IllegalArgumentException if it contains a value that not suitable for the array element
     */
    public static Array of(@Nonnull Object... elements) {
        Objects.requireNonNull(elements);
        return fromList(Arrays.asList(elements));
    }

    /**
     * Returns the elements.
     * @return the elements
     */
    public List<Value> getElements() {
        return elements;
    }

    @Override
    public Iterator<Value> iterator() {
        return elements.iterator();
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements);
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
        Array other = (Array) obj;
        return Objects.equals(elements, other.elements);
    }

    @Override
    public String toString() {
        return elements.stream()
                .map(Value::toString)
                .collect(Collectors.joining(", ", "[", "]")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }
}
