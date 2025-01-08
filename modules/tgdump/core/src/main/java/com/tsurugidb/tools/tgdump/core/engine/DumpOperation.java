/*
 * Copyright 2023-2024 Project Tsurugi.
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
package com.tsurugidb.tools.tgdump.core.engine;

import java.util.List;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;

/**
 * Individual dump operations for target types, used in {@link BasicDumpSession}.
 * @see BasicDumpSession
 */
interface DumpOperation {

    /**
     * Returns whether this has no registered operations.
     * @return {@code true} if this has no registered operations, otherwise {@code false}
     * @see #register(SqlClient, DumpMonitor, DumpTarget)
     */
    boolean isEmpty();

    /**
     * Returns a list of the dump target table names.
     * @return the table names, or empty if they are not sure in this operation type
     */
    default List<String> getTargetTables() {
        return List.of();
    }

    /**
     * Registers a dump operation.
     * @param client the SQL client to access the database
     * @param monitor the operation monitor
     * @param target the dump target table information
     * @throws InterruptedException if interrupted during the operation
     * @throws DiagnosticException if the target table is not found in the database
     * @throws DiagnosticException if error was occurred
     * @throws IllegalStateException if the transaction have been already started
     */
    void register(@Nonnull SqlClient client, @Nonnull DumpMonitor monitor, @Nonnull DumpTarget target)
            throws InterruptedException, DiagnosticException;

    /**
     * Executes a dump operation for the table.
     * @param client the SQL client to access the database
     * @param transaction the transaction where the operation is executed
     * @param monitor the operation monitor
     * @param target the dump target information
     * @throws InterruptedException if interrupted during the operation
     * @throws DiagnosticException if error was occurred
     * @throws IllegalArgumentException if the dump target is not {@link #register(SqlClient, DumpMonitor, DumpTarget) registered}
     * @throws IllegalStateException if the transaction have not been started. or already finished
     */
    void execute(
            @Nonnull SqlClient client,
            @Nonnull Transaction transaction,
            @Nonnull DumpMonitor monitor,
            @Nonnull DumpTarget target)
            throws InterruptedException, DiagnosticException;
}
