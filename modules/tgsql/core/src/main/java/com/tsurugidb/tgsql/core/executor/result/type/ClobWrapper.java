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
package com.tsurugidb.tgsql.core.executor.result.type;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.ClobReference;
import com.tsurugidb.tsubakuro.sql.Transaction;

/**
 * CLOB.
 */
public final class ClobWrapper implements IdWrapper {

    /**
     * object name prefix.
     */
    public static final String PREFIX = "clob";

    private static final AtomicInteger SEED = new AtomicInteger();

    public static ClobWrapper of(ClobReference reference) {
        int id = SEED.getAndIncrement();
        return new ClobWrapper(id, reference);
    }

    private final int id;
    private final ClobReference reference;

    private ClobWrapper(int id, ClobReference reference) {
        this.id = id;
        this.reference = reference;
    }

    @Override
    public int id() {
        return this.id;
    }

    public void copyTo(Transaction transaction, Path destination) throws IOException, ServerException, InterruptedException {
        transaction.copyTo(reference, destination).await();
    }

    @Override
    public String toString() {
        return PREFIX + "@" + id;
    }
}
