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
package com.tsurugidb.tgsql.core.model;

import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A value with region.
 * @param <V> the value type
 */
public final class Regioned<V> {

    private final V value;

    private final Region region;

    /**
     * Creates a new instance.
     * @param value the actual value
     * @param region the region of the value
     */
    public Regioned(@Nullable V value, @Nonnull Region region) {
        Objects.requireNonNull(region);
        this.value = value;
        this.region = region;
    }

    /**
     * Returns the value.
     * @return the value
     */
    public @Nullable V getValue() {
        return value;
    }

    /**
     * Returns the region.
     * @return the region
     */
    public Region getRegion() {
        return region;
    }

    /**
     * Returns the mapped object.
     * @param <R> the mapped value type
     * @param function the value mapper
     * @return the mapped object
     */
    public <R> Regioned<R> map(@Nonnull Function<? super V, ? extends R> function) {
        return new Regioned<>(function.apply(value), region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
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
        Regioned<?> other = (Regioned<?>) obj;
        return Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
