package com.tsurugidb.console.core.executor.engine;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.model.CommitStatement;
import com.tsurugidb.console.core.model.ErroneousStatement;
import com.tsurugidb.console.core.model.ErroneousStatement.ErrorKind;
import com.tsurugidb.console.core.model.Regioned;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.console.core.model.StartTransactionStatement;
import com.tsurugidb.console.core.model.StartTransactionStatement.ReadWriteMode;
import com.tsurugidb.console.core.model.StartTransactionStatement.TransactionMode;
import com.tsurugidb.sql.proto.SqlRequest;

/**
 * Utilities about Tsurugi SQL console executors.
 */
public final class ExecutorUtil {

    static final Logger LOG = LoggerFactory.getLogger(ExecutorUtil.class);

    private static final String COMMAND_EXIT = "exit"; //$NON-NLS-1$

    private static final String COMMAND_HALT = "halt"; //$NON-NLS-1$

    private static final String COMMAND_HELP = "help"; //$NON-NLS-1$

    private static final String COMMAND_HELP_SHORT = "h"; //$NON-NLS-1$

    private static final String COMMAND_STATUS = "status"; //$NON-NLS-1$

    /**
     * Extracts transaction option from the {@link StartTransactionStatement}.
     *
     * @param statement the extraction target statement
     * @param config    script configuration
     * @return the extracted option
     */
    public static SqlRequest.TransactionOption toTransactionOption(@Nonnull StartTransactionStatement statement, ScriptConfig config) {
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

    private static Optional<SqlRequest.TransactionType> computeTransactionType(StartTransactionStatement statement) {
        boolean ltx = unwrap(statement.getTransactionMode()) == TransactionMode.LONG //
                || unwrap(statement.getReadWriteMode()) == ReadWriteMode.READ_ONLY //
                || statement.getWritePreserve().isPresent() //
                || statement.getReadAreaInclude().isPresent() //
                || statement.getReadAreaExclude().isPresent();
        boolean ro = unwrap(statement.getReadWriteMode()) == ReadWriteMode.READ_ONLY_DEFERRABLE;
        if (ltx) {
            if (ro) {
                LOG.warn("transaction type is conflicted between LTX and RO; executes as LTX (line={}, column={})", //
                        statement.getRegion().getStartLine() + 1, //
                        statement.getRegion().getStartColumn() + 1);
            }
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

    /**
     * Returns whether or not the statement represents {@code '\exit'} command.
     *
     * @param statement the extraction target statement
     * @return {@code true} if the statement represents such the command, or {@code false} otherwise
     */
    public static boolean isExitCommand(@Nonnull SpecialStatement statement) {
        Objects.requireNonNull(statement);
        return isCommand(COMMAND_EXIT, statement);
    }

    /**
     * Returns whether or not the statement represents {@code '\halt'} command.
     *
     * @param statement the extraction target statement
     * @return {@code true} if the statement represents such the command, or {@code false} otherwise
     */
    public static boolean isHaltCommand(@Nonnull SpecialStatement statement) {
        Objects.requireNonNull(statement);
        return isCommand(COMMAND_HALT, statement);
    }

    /**
     * Returns whether or not the statement represents {@code '\help'} command.
     *
     * @param statement the extraction target statement
     * @return {@code true} if the statement represents such the command, or {@code false} otherwise
     */
    public static boolean isHelpCommand(@Nonnull SpecialStatement statement) {
        Objects.requireNonNull(statement);
        return isCommand(COMMAND_HELP, statement) || isCommand(COMMAND_HELP_SHORT, statement);
    }

    /**
     * Returns whether or not the statement represents {@code '\status'} command.
     *
     * @param statement the extraction target statement
     * @return {@code true} if the statement represents such the command, or {@code false} otherwise
     */
    public static boolean isStatusCommand(@Nonnull SpecialStatement statement) {
        Objects.requireNonNull(statement);
        return isCommand(COMMAND_STATUS, statement);
    }

    /**
     * Returns whether or not the statement represents the command.
     *
     * @param name      the command name
     * @param statement the extraction target statement
     * @return {@code true} if the statement represents such the command, or {@code false} otherwise
     */
    public static boolean isCommand(@Nonnull String name, @Nonnull SpecialStatement statement) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(statement);
        String extracted = statement.getCommandName().getValue();
        return name.equalsIgnoreCase(extracted);
    }

    /**
     * Returns an {@link ErroneousStatement} from the unknown command.
     *
     * @param statement the unknown command
     * @return corresponding {@link ErroneousStatement}
     */
    public static ErroneousStatement toUnknownError(@Nonnull SpecialStatement statement) {
        Objects.requireNonNull(statement);
        return new ErroneousStatement(//
                statement.getText(), //
                statement.getRegion(), //
                ErrorKind.UNKNOWN_SPECIAL_COMMAND, //
                statement.getCommandName().getRegion(), //
                MessageFormat.format(//
                        "unknown command: \"{0}\"", //
                        statement.getCommandName().getValue()));
    }

    /**
     * Returns an {@link ErroneousStatement} from the unknown command.
     *
     * @param statement the special command
     * @param option the unknown option token in the command
     * @return corresponding {@link ErroneousStatement}
     */
    public static ErroneousStatement toUnknownError(
            @Nonnull SpecialStatement statement,
            @Nonnull Regioned<String> option) {
        Objects.requireNonNull(statement);
        Objects.requireNonNull(option);
        return new ErroneousStatement(//
                statement.getText(), //
                statement.getRegion(), //
                ErrorKind.UNKNOWN_SPECIAL_COMMAND_OPTION, //
                option.getRegion(), //
                MessageFormat.format(//
                        "unrecognized option: \"{0}\"", //
                        option.getValue()));
    }

    private static <T> T unwrap(Optional<Regioned<T>> value) {
        return value.map(Regioned::getValue).orElse(null);
    }

    private ExecutorUtil() {
        throw new AssertionError();
    }
}