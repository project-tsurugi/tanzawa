/*
 * Copyright 2023-2026 Project Tsurugi.
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

import com.tsurugidb.tsubakuro.common.ShutdownType;

/**
 * Session shutdown type.
 */
public enum TgsqlShutdownType {
    /** No shutdown. */
    NOTHING(null),
    /** Waits for the ongoing requests and safely shutdown the session. */
    GRACEFUL(ShutdownType.GRACEFUL),
    /** Cancelling the ongoing requests and safely shutdown the session. */
    FORCEFUL(ShutdownType.FORCEFUL);

    private final ShutdownType type;

    TgsqlShutdownType(ShutdownType type) {
        this.type = type;
    }

    public ShutdownType getRawShutdownType() {
        return this.type;
    }
}
