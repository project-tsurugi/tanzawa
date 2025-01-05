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

import java.text.MessageFormat;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget.TargetType;
import com.tsurugidb.tsubakuro.sql.SqlClient;
import com.tsurugidb.tsubakuro.sql.Transaction;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An implementation of {@link DumpOperation} for dispatching by its tarrget type.
 */
class DumpOperationDispatch implements DumpOperation {

    private final Map<DumpTarget.TargetType, DumpOperation> elements;

    /**
     * Creates a new instance.
     * @param elements the elements to dispatch
     */
    DumpOperationDispatch(@NonNull Map<TargetType, DumpOperation> elements) {
        Objects.requireNonNull(elements);
        this.elements = elements.isEmpty() ? Map.of() : new EnumMap<>(elements);
    }

    /**
     * Returns the operation for the specified target type.
     * @param type the target type
     * @return the operation for the specified target type, or {@code empty} if not found
     */
    public Optional<DumpOperation> getOperation(@NonNull DumpTarget.TargetType type) {
        Objects.requireNonNull(type);
        return Optional.ofNullable(elements.get(type));
    }

    private DumpOperation getOperationStrict(@NonNull DumpTarget target) {
        return getOperation(target.getTargetType())
                .orElseThrow(() -> new UnsupportedOperationException(MessageFormat.format(
                    "unsupported target type: {0} in {1}", 
                    target.getTargetType(),
                    target.getLabel())));
    }

    @Override
    public List<String> getTargetTables() {
        return elements.values().stream()
                .flatMap(e -> e.getTargetTables().stream())
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean isEmpty() {
        return elements.values().stream().allMatch(DumpOperation::isEmpty);
    }

    @Override
    public void register(@NonNull SqlClient client, @NonNull DumpMonitor monitor, @NonNull DumpTarget target)
            throws InterruptedException, DiagnosticException {
        Objects.requireNonNull(client);
        Objects.requireNonNull(monitor);
        Objects.requireNonNull(target);
        getOperationStrict(target).register(client, monitor, target);
    }

    @Override
    public void execute(
            @NonNull SqlClient client,
            @NonNull Transaction transaction,
            @NonNull DumpMonitor monitor,
            @NonNull DumpTarget target)
            throws InterruptedException, DiagnosticException {
        Objects.requireNonNull(client);
        Objects.requireNonNull(transaction);
        Objects.requireNonNull(monitor);
        Objects.requireNonNull(target);
        getOperationStrict(target).execute(client, transaction, monitor, target);
    }
}
