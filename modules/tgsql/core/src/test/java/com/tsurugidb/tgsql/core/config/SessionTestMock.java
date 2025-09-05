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
package com.tsurugidb.tgsql.core.config;

import java.io.IOException;

import com.tsurugidb.tsubakuro.channel.common.connection.wire.Wire;
import com.tsurugidb.tsubakuro.common.Session;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.util.ServerResource;
import com.tsurugidb.tsubakuro.util.Timeout;

public class SessionTestMock implements Session {

    @Override
    public void connect(Wire sessionWire) {
        throw new UnsupportedOperationException("do override");
    }

    @Override
    public Wire getWire() {
        throw new UnsupportedOperationException("do override");
    }

    @Override
    public Timeout getCloseTimeout() {
        throw new UnsupportedOperationException("do override");
    }

    @Override
    public void close() throws ServerException, IOException, InterruptedException {
    }

    @Override
    public void put(ServerResource resource) {
        throw new UnsupportedOperationException("do override");
    }

    @Override
    public void remove(ServerResource resource) {
        throw new UnsupportedOperationException("do override");
    }
}
