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
package com.tsurugidb.tools.tgdump.core.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.TextFormat;
import com.tsurugidb.sql.proto.SqlCommon;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;
import com.tsurugidb.tools.tgdump.core.model.DumpProfile;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.TableMetadata;
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.exception.CompileException;
import com.tsurugidb.tsubakuro.sql.exception.TargetNotFoundException;

/**
 * An implementation of {@link DumpOperation} for table dump.
 */
class TableDumpOperation implements DumpOperation {

    private static final Logger LOG = LoggerFactory.getLogger(TableDumpOperation.class);

    private static final String SQL_DUMP_QUERY = "SELECT * FROM %s"; //$NON-NLS-1$

    private final Map<String, TableMetadata> registered = new ConcurrentHashMap<>();

    private final DumpProfile dumpProfile;

    private final boolean createTargetDirectories;

    /**
     * Creates a new instance.
     * @param dumpProfile the dump operation settings
     * @param createTargetDirectories whether or not to create dump target directories before the dump operations
     */
    TableDumpOperation(@Nonnull DumpProfile dumpProfile, boolean createTargetDirectories) {
        Objects.requireNonNull(dumpProfile);
        this.dumpProfile = dumpProfile;
        this.createTargetDirectories = createTargetDirectories;
    }

    @Override
    public boolean isEmpty() {
        return registered.isEmpty();
    }

    @Override
    public List<String> getTargetTables() {
        return List.copyOf(registered.keySet());
    }

    private static void checkTargetType(DumpTarget target) {
        if (target.getTargetType() != DumpTarget.TargetType.TABLE) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    "target type must be {0}: {1} ({2})",
                    DumpTarget.TargetType.TABLE,
                    target.getTargetType(),
                    target.getLabel()));
        }
    }

    @Override
    public void register(@Nonnull SqlClient client, @Nonnull DumpMonitor monitor, @Nonnull DumpTarget target)
            throws InterruptedException, DiagnosticException {
        Objects.requireNonNull(client);
        Objects.requireNonNull(monitor);
        Objects.requireNonNull(target);
        checkTargetType(target);
        String table = target.getTableName();
        if (registered.containsKey(table)) {
            monitor.verbose("skip already registerd table: {0}", table); //$NON-NLS-1$
            return;
        }
        monitor.verbose("inspecting dump table: {0} ({1})", table, target.getDestination()); //$NON-NLS-1$
        try {
            var metadata = client.getTableMetadata(table).await();
            registered.put(table, metadata);
            monitor.onDumpInfo(table, metadata, target.getDestination());
        } catch (TargetNotFoundException e) {
            LOG.debug("exception was occurred in prepare", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.TABLE_NOT_FOUND, List.of(table), e);
        } catch (IOException e) {
            LOG.debug("exception was occurred in prepare", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.IO_ERROR, List.of(e.toString()), e);
        } catch (ServerException e) {
            LOG.debug("exception was occurred in prepare", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.SERVER_ERROR, List.of(DiagnosticUtil.getMessage(e)), e);
        }
    }

    @Override
    public void execute(
            @Nonnull SqlClient client,
            @Nonnull Transaction transaction,
            @Nonnull DumpMonitor monitor,
            @Nonnull DumpTarget target)
            throws InterruptedException, DiagnosticException {
        Objects.requireNonNull(client);
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(monitor);
        Objects.requireNonNull(target);
        checkTargetType(target);
        var table = target.getTableName();
        if (!registered.containsKey(table)) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "the table {0} has not been prepared",
                    table));
        }
        var statement = createStatement(table);
        monitor.verbose("preparing dump command: {0} ({1}: {2})", //$NON-NLS-1$
                table, transaction.getTransactionId(), statement);
        var dumpOptions = dumpProfile.toProtocolBuffer();
        if (LOG.isDebugEnabled()) {
            LOG.debug("dump options: {}", TextFormat.shortDebugString(dumpOptions));
        }
        try (var prepared = client.prepare(statement, List.of()).await()) {
            monitor.onDumpStart(table, target.getDestination());

            // create target directory
            if (createTargetDirectories) {
                LOG.debug("creating dump target directory: {} ({})", table, target.getDestination());
                Files.createDirectories(target.getDestination());
            }

            try (var rs = transaction.executeDump(prepared, List.of(), target.getDestination(), dumpOptions).await()) {
                monitor.verbose("start retrieving dump results: {0} ({1})", //$NON-NLS-1$
                        table, transaction.getTransactionId());
                // NOTE: we assume the first column has provided dump file path (from operation specification).
                var meta = rs.getMetadata();
                if (meta.getColumns().isEmpty()) {
                    // may not occur in general cases
                    throw new IllegalStateException("invalid result set format: column list is empty");
                }
                var column = meta.getColumns().get(0);
                if (column.getTypeInfoCase() != SqlCommon.Column.TypeInfoCase.ATOM_TYPE
                        || column.getAtomType() != SqlCommon.AtomType.CHARACTER) {
                    // may not occur otherwise dump operation specification was changed
                    throw new IllegalStateException(MessageFormat.format(
                            "unexpected dump result type: {0} (expected: {1})",
                            column,
                            SqlCommon.AtomType.CHARACTER));
                }
                while (rs.nextRow()) {
                    if (!rs.nextColumn()) {
                        throw new IllegalStateException("broken dump result (less columns in the result set)");
                    }
                    var file = Path.of(rs.fetchCharacterValue());
                    monitor.onDumpFile(table, file);
                }
            } catch (ServerException e) {
                LOG.debug("exception was occurred in execute", e); //$NON-NLS-1$
                throw new DumpException(DumpDiagnosticCode.OPERATION_FAILURE,
                        List.of(table, statement), e);
            }
            monitor.onDumpFinish(table, target.getDestination());
        } catch (IOException e) {
            LOG.debug("exception was occurred in execute", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.IO_ERROR, List.of(e.toString()), e);
        } catch (CompileException e) {
            LOG.debug("exception was occurred in prepare", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.PREPARE_FAILURE, List.of(table, statement), e);
        } catch (ServerException e) {
            LOG.debug("exception was occurred in execute", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.SERVER_ERROR, List.of(DiagnosticUtil.getMessage(e)), e);
        }
    }

    private static final Pattern PATTERN_REGULAR_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*"); //$NON-NLS-1$

    private static String createStatement(String tableName) {
        var matcher = PATTERN_REGULAR_IDENTIFIER.matcher(tableName);
        if (matcher.matches()) {
            return String.format(SQL_DUMP_QUERY, tableName);
        }
        var string = new StringBuilder();
        string.append('"');
        for (int i = 0; i < tableName.length(); i++) {
            var c = tableName.charAt(i);
            if (c == '"') {
                string.append('"');
                string.append('"');
            } else {
                string.append(c);
            }
        }
        string.append('"');
        return String.format(SQL_DUMP_QUERY, string.toString());
    }
}
