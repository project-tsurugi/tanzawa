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
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.TransactionOption;
import com.tsurugidb.tgsql.core.exception.TgsqlMessageException;
import com.tsurugidb.tgsql.core.executor.result.type.IdWrapper;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.Transaction;

/**
 * Transaction with transaction option.
 */
public class TransactionWrapper implements AutoCloseable {

    private final Transaction transaction;
    private final TransactionOption option;
    private final Map<Class<?>, Map<Integer, IdWrapper>> objectMap = new HashMap<>();

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
    public <T extends IdWrapper> void addObject(Class<T> key, T value) {
        if (value == null) {
            return;
        }

        var list = objectMap.computeIfAbsent(key, k -> new TreeMap<>());
        list.put(value.id(), value);
    }

    /**
     * get object map.
     *
     * @param <T> object type
     * @param key key
     * @return object map
     */
    @SuppressWarnings("unchecked")
    public <T extends IdWrapper> Map<Integer, T> objectMap(Class<T> key) {
        var map = objectMap.get(key);
        if (map == null) {
            return Map.of();
        }
        return (Map<Integer, T>) map;
    }

    /**
     * get object list.
     *
     * @param <T> object type
     * @param key key
     * @return object list
     */
    public <T extends IdWrapper> Collection<T> objectList(Class<T> key) {
        return objectMap(key).values();
    }

    /**
     * get object.
     *
     * @param <T>    object type
     * @param key    key
     * @param prefix prefix
     * @param id     object id
     * @return object list
     * @throws TgsqlMessageException if object not found
     */
    public <T extends IdWrapper> T getObject(Class<T> key, String prefix, int id) {
        var map = objectMap(key);
        T object = map.get(id);
        if (object == null) {
            throw new TgsqlMessageException(MessageFormat.format("object not found in transaction. objectName={0}@{1}", prefix, id));
        }
        return object;
    }

    @Override
    public void close() throws ServerException, IOException, InterruptedException {
        transaction.close();
    }
}
