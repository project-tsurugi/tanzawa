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
package com.tsurugidb.tgsql.core.exception;

/**
 * script no message exception.
 *
 * <p>
 * The message is output at the location where this exception is thrown. The caught side does nothing.
 * </p>
 */
@SuppressWarnings("serial")
public class TgsqlNoMessageException extends RuntimeException {

    /**
     * Creates a new instance.
     *
     * @param cause the cause
     */
    public TgsqlNoMessageException(Throwable cause) {
        super(cause);
    }
}
