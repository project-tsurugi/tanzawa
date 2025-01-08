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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
import com.tsurugidb.tsubakuro.sql.Transaction;
import com.tsurugidb.tsubakuro.sql.exception.CompileException;

/**
 * An implementation of {@link DumpOperation} for SQL text.
 */
class QueryDumpOperation implements DumpOperation {

    private static final Logger LOG = LoggerFactory.getLogger(QueryDumpOperation.class);

    private final Set<String> registered = ConcurrentHashMap.newKeySet();

    private final DumpProfile dumpProfile;

    private final boolean createTargetDirectories;

    /**
     * Creates a new instance.
     * @param dumpProfile the dump operation settings
     * @param createTargetDirectories whether or not to create dump target directories before the dump operations
     */
    QueryDumpOperation(@Nonnull DumpProfile dumpProfile, boolean createTargetDirectories) {
        Objects.requireNonNull(dumpProfile);
        this.dumpProfile = dumpProfile;
        this.createTargetDirectories = createTargetDirectories;
    }

    @Override
    public boolean isEmpty() {
        return registered.isEmpty();
    }

    private static void checkTargetType(DumpTarget target) {
        if (target.getTargetType() != DumpTarget.TargetType.QUERY) {
            throw new UnsupportedOperationException(MessageFormat.format(
                    "target type must be {0}: {1} ({2})",
                    DumpTarget.TargetType.QUERY,
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
        var label = target.getLabel();
        var statement = target.getTarget();
        if (registered.contains(statement)) {
            monitor.verbose("skip already registerd query: {1} ({0})", label, statement); //$NON-NLS-1$
            return;
        }
        monitor.verbose("validating query: {0} ({1})", label, target.getDestination()); //$NON-NLS-1$
        try (var prepared = client.prepare(statement, List.of()).await()) {
            // NOTE: we don't keep the prepared SQL statement to simplify the server-side resource management
            registered.add(statement);
            monitor.onDumpInfo(label, statement, target.getDestination());
        } catch (CompileException e) {
            LOG.debug("exception was occurred in prepare", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.PREPARE_FAILURE, List.of(label, statement), e);
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
        var label = target.getLabel();
        var statement = target.getTarget();
        if (!registered.contains(statement)) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "the query \"{1}\" ({0}) has not been prepared",
                    label,
                    statement));
        }
        monitor.verbose("preparing dump command: {0} ({1}: {2})", //$NON-NLS-1$
                label, transaction.getTransactionId(), statement);
        var dumpOptions = dumpProfile.toProtocolBuffer();
        if (LOG.isDebugEnabled()) {
            LOG.debug("dump options: {}", TextFormat.shortDebugString(dumpOptions));
        }
        try (var prepared = client.prepare(statement, List.of()).await()) {
            monitor.onDumpStart(label, target.getDestination());

            // create target directory
            if (createTargetDirectories) {
                LOG.debug("creating dump target directory: {} ({})", label, target.getDestination());
                Files.createDirectories(target.getDestination());
            }

            try (var rs = transaction.executeDump(prepared, List.of(), target.getDestination(), dumpOptions).await()) {
                monitor.verbose("start retrieving dump results: {0} ({1})", //$NON-NLS-1$
                        label, transaction.getTransactionId());
                // NOTE: we assume the first column has provided dump file path (from operation specification).
                var meta = rs.getMetadata();
                if (meta.getColumns().isEmpty()) {
                    // may not occur in general cases
                    throw new IllegalStateException("invalid result set format: colum list is empty");
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
                    monitor.onDumpFile(label, file);
                }
            } catch (ServerException e) {
                LOG.debug("exception was occurred in execute", e); //$NON-NLS-1$
                throw new DumpException(DumpDiagnosticCode.OPERATION_FAILURE,
                        List.of(label, statement), e);
            }
            monitor.onDumpFinish(label, target.getDestination());
        } catch (IOException e) {
            LOG.debug("exception was occurred in execute", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.IO_ERROR, List.of(e.toString()), e);
        } catch (CompileException e) {
            LOG.debug("exception was occurred in prepare", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.PREPARE_FAILURE, List.of(label, statement), e);
        } catch (ServerException e) {
            LOG.debug("exception was occurred in execute", e); //$NON-NLS-1$
            throw new DumpException(DumpDiagnosticCode.SERVER_ERROR, List.of(DiagnosticUtil.getMessage(e)), e);
        }
    }
}
