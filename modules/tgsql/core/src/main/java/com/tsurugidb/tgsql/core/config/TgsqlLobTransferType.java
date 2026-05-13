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

import com.tsurugidb.tsubakuro.common.BlobTransferType;

/**
 * Large object transfer type.
 */
public enum TgsqlLobTransferType {
    /** Default transfer policy. */
    DEFAULT(BlobTransferType.DEFAULT),
    /** Does not use transfer type. */
    NOT_USE(BlobTransferType.DOES_NOT_USE),
    /** Privileged transfer type. */
    PRIVILEGED(BlobTransferType.PRIVILEGED),
    /** Blob Relay transfer type. */
    RELAY(BlobTransferType.RELAY);

    private final BlobTransferType type;

    TgsqlLobTransferType(BlobTransferType type) {
        this.type = type;
    }

    public BlobTransferType getRawBlobTransferType() {
        return this.type;
    }

    public static TgsqlLobTransferType valueOf(BlobTransferType rawLobTransferType) {
        for (TgsqlLobTransferType lobTransferType : values()) {
            if (lobTransferType.type == rawLobTransferType) {
                return lobTransferType;
            }
        }
        return null;
    }
}
