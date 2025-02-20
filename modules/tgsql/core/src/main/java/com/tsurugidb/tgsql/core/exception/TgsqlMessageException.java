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
 * script message exception.
 */
@SuppressWarnings("serial")
public class TgsqlMessageException extends RuntimeException {

    private long timingTime;

    /**
     * Creates a new instance.
     *
     * @param message the detail message
     */
    public TgsqlMessageException(String message) {
        super(message);
    }

    /**
     * Creates a new instance.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public TgsqlMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance.
     *
     * @param message the detail message
     * @param cause   the cause
     * @param time    time
     */
    public TgsqlMessageException(String message, Throwable cause, long time) {
        super(message, cause);
        this.timingTime = time;
    }

    /**
     * get time.
     *
     * @return time
     */
    public long getTimingTime() {
        return this.timingTime;
    }
}
