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
package com.tsurugidb.tools.tgdump.cli;

/**
 * The application constants of Tsurugi Dump Tool.
 */
public final class Constants {

    /**
     * The application name.
     */
    public static final String APPLICATION_NAME = "tgdump";

    /**
     * The exit status value of successful.
     */
    public static final int EXIT_STATUS_OK = 0;

    /**
     * The exit status value of operation errors.
     */
    public static final int EXIT_STATUS_OPERATION_ERROR = 1;

    /**
     * The exit status value of parameter errors.
     */
    public static final int EXIT_STATUS_PARAMETER_ERROR = 2;

    /**
     * The exit status value of monitoring errors.
     */
    public static final int EXIT_STATUS_MONITOR_ERROR = 3;

    /**
     * The exit status value of internal errors.
     */
    public static final int EXIT_STATUS_INTERNAL_ERROR = 4;

    /**
     * The exit status value of operation interrupted.
     */
    public static final int EXIT_STATUS_INTERRUPTED = 5;

    private Constants() {
        return;
    }
}
