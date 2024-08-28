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
package com.tsurugidb.tgsql.core.executor.result;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.Nonnull;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.ResultSet;

/**
 * Processes {@link ResultSet}.
 */
@FunctionalInterface
public interface ResultProcessor extends Closeable {

    /**
     * Processes {@link ResultSet}.
     *
     * @param target the target result set
     * @return {@link System#nanoTime()}
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while processing the result set
     * @throws InterruptedException if interrupted while processing the result set
     */
    long process(@Nonnull ResultSet target) throws ServerException, IOException, InterruptedException;

    @Override
    default void close() throws IOException {
        return;
    }
}
