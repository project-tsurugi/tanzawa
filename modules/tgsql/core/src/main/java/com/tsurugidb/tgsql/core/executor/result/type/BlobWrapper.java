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

import com.tsurugidb.tsubakuro.sql.BlobReference;
import com.tsurugidb.tsubakuro.sql.SqlClient;

/**
 * BLOB.
 */
public final class BlobWrapper implements IdWrapper {

    /**
     * object name prefix.
     */
    public static final String PREFIX = "blob";

    private static final AtomicInteger SEED = new AtomicInteger();

    public static BlobWrapper of(BlobReference reference) {
        int id = SEED.getAndIncrement();
        return new BlobWrapper(id, reference);
    }

    private final int id;
    private final BlobReference reference;

    private BlobWrapper(int id, BlobReference reference) {
        this.id = id;
        this.reference = reference;
    }

    @Override
    public int id() {
        return this.id;
    }

    public void copyTo(SqlClient client, Path destination) throws IOException {
        client.copyTo(reference, destination);
    }

    @Override
    public String toString() {
        return PREFIX + "@" + id;
    }
}
