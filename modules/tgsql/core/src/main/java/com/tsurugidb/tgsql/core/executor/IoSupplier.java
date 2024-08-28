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
package com.tsurugidb.tgsql.core.executor;

import java.io.IOException;

/**
 * Supplies an I/O object.
 * @param <T> the object type.
 */
@FunctionalInterface
public interface IoSupplier<T> {

    /**
     * Supplies an object.
     * @return the supplies object
     * @throws IOException if I/O error occurred while supplying the object
     */
    T get() throws IOException;
}
