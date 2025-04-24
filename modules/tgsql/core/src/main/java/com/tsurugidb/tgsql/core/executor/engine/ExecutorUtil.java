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
package com.tsurugidb.tgsql.core.executor.engine;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.config.TgsqlCvKey;
import com.tsurugidb.tgsql.core.model.CommitStatement;
import com.tsurugidb.tgsql.core.model.Regioned;
import com.tsurugidb.tgsql.core.model.StartTransactionStatement;
import com.tsurugidb.tgsql.core.model.StartTransactionStatement.ReadWriteMode;
import com.tsurugidb.tgsql.core.model.StartTransactionStatement.TransactionMode;
import com.tsurugidb.tgsql.core.model.Value;

/**
 * Utilities about Tsurugi SQL console executors.
 */
public final class ExecutorUtil {

    static final Logger LOG = LoggerFactory.getLogger(ExecutorUtil.class);

    /**
     * Extracts transaction option from the {@link StartTransactionStatement}.
     *
     * @param statement the extraction target statement
     * @param config    tgsql configuration
     * @return the extracted option
     * @throws EngineException if error occurred in engine itself
     */
    public static SqlRequest.TransactionOption toTransactionOption(@Nonnull StartTransactionStatement statement, TgsqlConfig config) throws EngineException {
        Objects.requireNonNull(statement);
        var options = SqlRequest.TransactionOption.newBuilder();
        computeTransactionType(statement).ifPresent(options::setType);
        computeTransactionPriority(statement).ifPresent(options::setPriority);
        computeLabel(statement, config).ifPresent(options::setLabel);
        computeWritePreserve(statement).ifPresent(options::addAllWritePreserves);
        computeIncludeDdl(statement, options.getType()).ifPresent(options::setModifiesDefinitions);
        computeInclusiveReadArea(statement).ifPresent(options::addAllInclusiveReadAreas);
        computeExclusiveReadArea(statement).ifPresent(options::addAllExclusiveReadAreas);
        computeProperties(statement, config, options);
        return options.build();
    }

    private static Optional<SqlRequest.TransactionType> computeTransactionType(StartTransactionStatement statement) throws EngineException {
        TransactionMode transactionMode = unwrap(statement.getTransactionMode());
        ReadWriteMode readWriteMode = unwrap(statement.getReadWriteMode());

        if (transactionMode == TransactionMode.LONG) {
            if (readWriteMode == ReadWriteMode.READ_ONLY_DEFERRABLE) {
                LOG.debug("transaction type is conflicted between LTX and RO (line={}, column={})", //
                        statement.getRegion().getStartLine() + 1, //
                        statement.getRegion().getStartColumn() + 1);
                throw new EngineException("transaction type is conflicted between \"LONG\" and \"READ ONLY\"");
            }
        }
        if (statement.getWritePreserve().isPresent()) {
            if (readWriteMode == ReadWriteMode.READ_ONLY_IMMEDIATE || readWriteMode == ReadWriteMode.READ_ONLY_DEFERRABLE) {
                LOG.debug("transaction type is conflicted between LTX and RO (line={}, column={})", //
                        statement.getRegion().getStartLine() + 1, //
                        statement.getRegion().getStartColumn() + 1);
                throw new EngineException("transaction type is conflicted between \"READ ONLY\" and \"WRITE PRESERVE\"");
            }
        }

        boolean ltx = transactionMode == TransactionMode.LONG //
                || readWriteMode == ReadWriteMode.READ_ONLY_IMMEDIATE //
                || statement.getWritePreserve().isPresent() //
                || statement.getReadAreaInclude().isPresent() //
                || statement.getReadAreaExclude().isPresent();
        boolean ro = readWriteMode == ReadWriteMode.READ_ONLY_DEFERRABLE;
        if (ltx) {
            return Optional.of(SqlRequest.TransactionType.LONG);
        }
        if (ro) {
            return Optional.of(SqlRequest.TransactionType.READ_ONLY);
        }
        return Optional.of(SqlRequest.TransactionType.SHORT);
    }

    private static Optional<SqlRequest.TransactionPriority> computeTransactionPriority(StartTransactionStatement statement) {
        if (statement.getExclusiveMode().isEmpty()) {
            return Optional.empty();
        }
        switch (statement.getExclusiveMode().get().getValue()) {
        case PRIOR_DEFERRABLE:
            return Optional.of(SqlRequest.TransactionPriority.WAIT);
        case PRIOR_IMMEDIATE:
            return Optional.of(SqlRequest.TransactionPriority.INTERRUPT);
        case EXCLUDING_DEFERRABLE:
            return Optional.of(SqlRequest.TransactionPriority.WAIT_EXCLUDE);
        case EXCLUDING_IMMEDIATE:
            return Optional.of(SqlRequest.TransactionPriority.INTERRUPT_EXCLUDE);
        }
        throw new AssertionError();
    }

    private static Optional<String> computeLabel(StartTransactionStatement statement, TgsqlConfig config) {
        if (statement.getLabel().isEmpty()) {
            return Optional.empty();
        }
        String label = statement.getLabel().get().getValue();
        var format = config.getClientVariableMap().get(TgsqlCvKey.TX_LABEL_SUFFIX_TIME);
        if (format != null) {
            label += format.now();
        }
        return Optional.of(label);
    }

    private static Optional<List<SqlRequest.WritePreserve>> computeWritePreserve(StartTransactionStatement statement) {
        if (statement.getWritePreserve().isEmpty()) {
            return Optional.empty();
        }
        var wps = statement.getWritePreserve().get().stream() //
                .map(Regioned::getValue) //
                .map(it -> SqlRequest.WritePreserve.newBuilder().setTableName(it).build()) //
                .collect(Collectors.toList());
        return Optional.of(wps);
    }

    private static Optional<Boolean> computeIncludeDdl(StartTransactionStatement statement, SqlRequest.TransactionType transactionType) throws EngineException {
        if (statement.getIncludeDdl().isEmpty()) {
            return Optional.empty();
        }
        boolean ddl = statement.getIncludeDdl().get().getValue();
        if (ddl) {
            ReadWriteMode readWriteMode = unwrap(statement.getReadWriteMode());
            if (readWriteMode == ReadWriteMode.READ_ONLY_DEFERRABLE || readWriteMode == ReadWriteMode.READ_ONLY_IMMEDIATE) {
                LOG.debug("include ddl is conflicted RO (line={}, column={})", //
                        statement.getRegion().getStartLine() + 1, //
                        statement.getRegion().getStartColumn() + 1);
                throw new EngineException("include ddl is conflicted \"READ ONLY\"");
            }

            if (transactionType != SqlRequest.TransactionType.LONG) {
                LOG.debug("include ddl is ignored (line={}, column={})", //
                        statement.getRegion().getStartLine() + 1, //
                        statement.getRegion().getStartColumn() + 1);
                return Optional.of(Boolean.FALSE);
            }
        }
        return Optional.of(ddl);
    }

    private static Optional<List<SqlRequest.ReadArea>> computeInclusiveReadArea(StartTransactionStatement statement) {
        return computeReadArea(statement.getReadAreaInclude());
    }

    private static Optional<List<SqlRequest.ReadArea>> computeExclusiveReadArea(StartTransactionStatement statement) {
        return computeReadArea(statement.getReadAreaExclude());
    }

    private static Optional<List<SqlRequest.ReadArea>> computeReadArea(Optional<List<Regioned<String>>> readArea) {
        if (readArea.isEmpty()) {
            return Optional.empty();
        }
        var ras = readArea.get().stream() //
                .map(Regioned::getValue) //
                .map(it -> SqlRequest.ReadArea.newBuilder().setTableName(it).build()) //
                .collect(Collectors.toList());
        return Optional.of(ras);
    }

    private interface PropertyProcessor {
        void execute(Value value, SqlRequest.TransactionOption.Builder options);

        void execute(String value, SqlRequest.TransactionOption.Builder options);
    }

    private static final Map<String, PropertyProcessor> PROPERTY_PROCESSOR_MAP;
    static {
        var map = new HashMap<String, PropertyProcessor>();
        map.put("PARALLEL", new PropertyProcessor() {
            @Override
            public void execute(Value value, SqlRequest.TransactionOption.Builder options) {
                execute(toInt(value), options);
            }

            @Override
            public void execute(String value, SqlRequest.TransactionOption.Builder options) {
                execute(toInt(value), options);
            }

            private void execute(Integer value, SqlRequest.TransactionOption.Builder options) {
                if (value != null) {
                    options.setScanParallel(value);
                }
            }
        });
        PROPERTY_PROCESSOR_MAP = map;
    }

    private static void computeProperties(StartTransactionStatement statement, TgsqlConfig config, SqlRequest.TransactionOption.Builder options) {
        var set = new HashSet<String>();
        {
            Map<String, Value> properties = unwrapMap(statement.getProperties());
            for (var entry : properties.entrySet()) {
                String key = entry.getKey().toUpperCase();
                set.add(key);

                var processor = PROPERTY_PROCESSOR_MAP.get(key);
                if (processor != null) {
                    processor.execute(entry.getValue(), options);
                } else {
                    LOG.debug("ignore property {}", entry.getKey());
                }
            }
        }
        {
            Map<String, String> properties = config.getProperty();
            for (var entry : properties.entrySet()) {
                String key = entry.getKey().toUpperCase();
                if (set.contains(key)) {
                    continue;
                }

                var processor = PROPERTY_PROCESSOR_MAP.get(key);
                if (processor != null) {
                    processor.execute(entry.getValue(), options);
                } else {
                    LOG.debug("ignore property {}", entry.getKey());
                }
            }
        }
    }

    /**
     * Compute transaction property.
     *
     * @param withMap properties
     * @param options transaction option builder
     */
    public static void computeProperties(Map<String, String> withMap, SqlRequest.TransactionOption.Builder options) {
        for (var entry : withMap.entrySet()) {
            String key = entry.getKey().trim().toUpperCase();

            var processor = PROPERTY_PROCESSOR_MAP.get(key);
            if (processor != null) {
                processor.execute(entry.getValue().trim(), options);
            } else {
                LOG.debug("ignore property {}", entry.getKey());
            }
        }
    }

    /**
     * Extracts commit option from the {@link CommitStatement}.
     *
     * @param statement the extraction target statement
     * @return the extracted option
     */
    public static Optional<SqlRequest.CommitStatus> toCommitStatus(@Nonnull CommitStatement statement) {
        Objects.requireNonNull(statement);
        if (statement.getCommitStatus().isEmpty()) {
            return Optional.empty();
        }
        switch (statement.getCommitStatus().get().getValue()) {
        case ACCEPTED:
            return Optional.of(SqlRequest.CommitStatus.ACCEPTED);
        case AVAILABLE:
            return Optional.of(SqlRequest.CommitStatus.AVAILABLE);
        case STORED:
            return Optional.of(SqlRequest.CommitStatus.STORED);
        case PROPAGATED:
            return Optional.of(SqlRequest.CommitStatus.PROPAGATED);
        }
        throw new AssertionError();
    }

    private static <T> T unwrap(Optional<Regioned<T>> value) {
        return value.map(Regioned::getValue).orElse(null);
    }

    private static <K> Map<K, Value> unwrapMap(Map<Regioned<K>, Optional<Regioned<Value>>> wrapped) {
        return wrapped.entrySet().stream().map(it -> Map.entry(it.getKey().getValue(), it.getValue().map(Regioned::getValue).orElse(Value.of())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static <K> Map<K, Value> unwrapMap(Optional<Map<Regioned<K>, Optional<Regioned<Value>>>> wrapped) {
        return wrapped.map(it -> unwrapMap(it)).orElse(Map.of());
    }

    private static Integer toInt(Value value) {
        switch (value.getKind()) {
        case NULL:
            return null;
        case CHARACTER:
            return value.asCharacter().map(ExecutorUtil::parseInt).orElse(null);
        case NUMERIC:
            return value.asNumeric().map(BigDecimal::intValue).orElse(null);
        case BOOLEAN:
            return value.asBoolean().map(b -> b.toString()).map(ExecutorUtil::parseInt).orElse(null);
        default:
            return Integer.parseInt(value.toString());
        }
    }

    private static Integer toInt(String value) {
        return parseInt(value);
    }

    private static Integer parseInt(String s) {
        if (s == null) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new RuntimeException(MessageFormat.format("\"{0}\" is not integer", s));
        }
    }

    private ExecutorUtil() {
        throw new AssertionError();
    }
}
