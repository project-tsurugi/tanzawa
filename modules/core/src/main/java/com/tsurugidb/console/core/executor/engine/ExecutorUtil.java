package com.tsurugidb.console.core.executor.engine;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.model.CommitStatement;
import com.tsurugidb.console.core.model.Regioned;
import com.tsurugidb.console.core.model.StartTransactionStatement;
import com.tsurugidb.console.core.model.StartTransactionStatement.ReadWriteMode;
import com.tsurugidb.console.core.model.StartTransactionStatement.TransactionMode;
import com.tsurugidb.sql.proto.SqlRequest;

/**
 * Utilities about Tsurugi SQL console executors.
 */
public final class ExecutorUtil {

    static final Logger LOG = LoggerFactory.getLogger(ExecutorUtil.class);

    /**
     * Extracts transaction option from the {@link StartTransactionStatement}.
     *
     * @param statement the extraction target statement
     * @param config    script configuration
     * @return the extracted option
     * @throws EngineException if error occurred in engine itself
     */
    public static SqlRequest.TransactionOption toTransactionOption(@Nonnull StartTransactionStatement statement, ScriptConfig config) throws EngineException {
        Objects.requireNonNull(statement);
        var options = SqlRequest.TransactionOption.newBuilder();
        computeTransactionType(statement).ifPresent(options::setType);
        computeTransactionPriority(statement).ifPresent(options::setPriority);
        statement.getLabel().ifPresent(it -> options.setLabel(it.getValue()));
        computeWritePreserve(statement).ifPresent(options::addAllWritePreserves);
        // FIXME: read area
        // FIXME: properties config.getProperty();
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
        return Optional.empty();
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

    private ExecutorUtil() {
        throw new AssertionError();
    }
}
