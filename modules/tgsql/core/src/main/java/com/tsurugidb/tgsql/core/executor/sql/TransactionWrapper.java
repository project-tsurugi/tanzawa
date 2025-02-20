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
package com.tsurugidb.tgsql.core.executor.sql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.TransactionOption;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.Transaction;

/**
 * Transaction with transaction option.
 */
public class TransactionWrapper implements AutoCloseable {

    private final Transaction transaction;
    private final TransactionOption option;
    private final Map<String, List<Object>> objectMap = new HashMap<>();

    /**
     * Creates a new instance.
     *
     * @param transaction transaction
     * @param option      transaction option
     */
    public TransactionWrapper(Transaction transaction, SqlRequest.TransactionOption option) {
        this.transaction = transaction;
        this.option = option;
    }

    /**
     * get transaction.
     *
     * @return transaction
     */
    public Transaction getTransaction() {
        return this.transaction;
    }

    /**
     * get transaction option.
     *
     * @return transaction option
     */
    public SqlRequest.TransactionOption getOption() {
        return this.option;
    }

    /**
     * add object.
     *
     * @param value object
     */
    public void addObject(String keyName, Object value) {
        if (value == null) {
            return;
        }

        var list = objectMap.computeIfAbsent(keyName, k -> new ArrayList<>());
        list.add(value);
    }

    /**
     * get object list
     *
     * @param <T>     object type
     * @param keyName key name
     * @return object list
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> objectList(String keyName) {
        var list = objectMap.get(keyName);
        if (list == null) {
            return List.of();
        }
        return (List<T>) list;
    }

    @Override
    public void close() throws ServerException, IOException, InterruptedException {
        transaction.close();
    }
}
