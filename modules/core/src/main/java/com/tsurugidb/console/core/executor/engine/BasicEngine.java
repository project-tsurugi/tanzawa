package com.tsurugidb.console.core.executor.engine;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.config.ScriptCommitMode;
import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.executor.engine.command.SpecialCommand;
import com.tsurugidb.console.core.executor.report.ScriptReporter;
import com.tsurugidb.console.core.executor.result.ResultProcessor;
import com.tsurugidb.console.core.executor.sql.SqlProcessor;
import com.tsurugidb.console.core.model.CallStatement;
import com.tsurugidb.console.core.model.CommitStatement;
import com.tsurugidb.console.core.model.ErroneousStatement;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.console.core.model.StartTransactionStatement;
import com.tsurugidb.console.core.model.Statement;
import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.tsubakuro.exception.ServerException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A basic implementation of {@link Engine}. Clients must start/finish transactions manually.
 */
public class BasicEngine extends AbstractEngine {

    static final Logger LOG = LoggerFactory.getLogger(BasicEngine.class);

    private final ScriptConfig config;

    private final SqlProcessor sqlProcessor;

    private final ResultProcessor resultSetProcessor;

    private final ScriptReporter reporter;

    /**
     * Creates a new instance.
     *
     * @param config             script configuration
     * @param sqlProcessor       the SQL processor
     * @param resultSetProcessor the result set processor
     * @param reporter           reporter
     */
    public BasicEngine(@Nonnull ScriptConfig config, @Nonnull SqlProcessor sqlProcessor, @Nonnull ResultProcessor resultSetProcessor, @Nonnull ScriptReporter reporter) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(sqlProcessor);
        Objects.requireNonNull(resultSetProcessor);
        Objects.requireNonNull(reporter);
        this.config = config;
        this.sqlProcessor = sqlProcessor;
        this.resultSetProcessor = resultSetProcessor;
        this.reporter = reporter;
    }

    /**
     * get sql processor.
     * 
     * @return sql processor
     */
    public SqlProcessor getSqlProcessor() {
        return this.sqlProcessor;
    }

    @Override
    public ScriptReporter getReporter() {
        return this.reporter;
    }

    @Override
    protected boolean executeEmptyStatement(@Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$
        return true;
    }

    @SuppressFBWarnings(value = "RCN", justification = "misdetection: SqlProcessor.execute() may return null")
    @Override
    protected boolean executeGenericStatement(@Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$

        checkTransactionActive(statement, true);
        try (var rs = sqlProcessor.execute(statement.getText(), statement.getRegion())) {
            if (rs != null) {
                resultSetProcessor.process(rs);
            }
        } catch (Exception e) {
            if (config.getCommitMode() == ScriptCommitMode.AUTO_COMMIT) {
                try {
                    executeRollbackImplicitly();
                } catch (Exception e1) {
                    e.addSuppressed(e1);
                }
            }
            throw e;
        }

        if (config.getCommitMode() == ScriptCommitMode.AUTO_COMMIT) {
            executeCommitImplicitly();
        }
        return true;
    }

    @Override
    protected boolean executeStartTransactionStatement(@Nonnull StartTransactionStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$

        checkTransactionInactive(statement);
        var option = ExecutorUtil.toTransactionOption(statement, config);
        sqlProcessor.startTransaction(option);
        reporter.reportTransactionStarted(option);
        return true;
    }

    @Override
    protected boolean executeCommitStatement(@Nonnull CommitStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$

        checkTransactionActive(statement, false);
        var status = ExecutorUtil.toCommitStatus(statement);
        sqlProcessor.commitTransaction(status.orElse(null));
        reporter.reportTransactionCommitted(status);
        return true;
    }

    protected void executeCommitImplicitly() throws ServerException, IOException, InterruptedException {
        var status = SqlRequest.CommitStatus.COMMIT_STATUS_UNSPECIFIED;
        sqlProcessor.commitTransaction(status);
        reporter.reportTransactionCommittedImplicitly(status);
    }

    @Override
    protected boolean executeRollbackStatement(@Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$

        checkTransactionActive(statement, false);
        sqlProcessor.rollbackTransaction();
        reporter.reportTransactionRollbacked();
        return true;
    }

    protected void executeRollbackImplicitly() throws ServerException, IOException, InterruptedException {
        sqlProcessor.rollbackTransaction();
        reporter.reportTransactionRollbackedImplicitly();
    }

    @Override
    protected boolean executeCallStatement(@Nonnull CallStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        // fall-back
        return executeGenericStatement(statement);
    }

    @Override
    protected boolean executeSpecialStatement(@Nonnull SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$

        var commandList = SpecialCommand.findCommand(statement);
        switch (commandList.size()) {
        case 0:
            // execute as erroneous
            LOG.debug("command is unrecognized: {}", statement.getCommandName()); //$NON-NLS-1$
            return execute(SpecialCommand.toUnknownError(statement));
        case 1:
            var command = commandList.get(0).command();
            LOG.debug("execute {}", command.getClass().getSimpleName());
            return command.execute(this, statement);
        default:
            // execute as erroneous
            List<String> nameList = commandList.stream().map(c -> c.name()).collect(Collectors.toList());
            LOG.debug("command is ambiguous: {} in {}", statement.getCommandName(), nameList); //$NON-NLS-1$
            return execute(SpecialCommand.toUnknownError(statement, nameList));
        }
    }

    @Override
    protected boolean executeErroneousStatement(@Nonnull ErroneousStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$

        throw new EngineException(MessageFormat.format("[{0}] {1} (line={2}, column={3})", //
                statement.getErrorKind(), statement.getMessage(), //
                statement.getOccurrence().getStartLine() + 1, statement.getOccurrence().getStartColumn() + 1));
    }

    @Override
    public void finish(boolean succeed) throws IOException {
        var commitMode = config.getCommitMode();
        LOG.debug("finish: succeed={}, commitMode={}", succeed, commitMode);
        try {
            switch (commitMode) {
            case COMMIT: // commit on success, rollback on failure
                if (sqlProcessor.isTransactionActive()) {
                    if (succeed) {
                        executeCommitImplicitly();
                    } else {
                        executeRollbackImplicitly();
                    }
                }
                break;
            case NO_COMMIT: // always rollback
                if (sqlProcessor.isTransactionActive()) {
                    executeRollbackImplicitly();
                }
                break;
            default:
                break;
            }
        } catch (ServerException | InterruptedException e) {
            throw new IOException(e);
        }
    }

    private void checkTransactionActive(Statement statement, boolean startIfInactive) throws EngineException, ServerException, IOException, InterruptedException {
        if (sqlProcessor.isTransactionActive()) {
            return;
        }

        if (startIfInactive) {
            var option = config.getTransactionOption();
            if (option != null) {
                reporter.reportStartTransactionImplicitly(option);
                sqlProcessor.startTransaction(option);
                return;
            }
        }

        throw new EngineException(MessageFormat.format("transaction is not started (line={0}, column={1})", //
                statement.getRegion().getStartLine() + 1, statement.getRegion().getStartColumn() + 1));
    }

    /**
     * Check transaction inactive.
     * 
     * @param statement the target statement
     * @throws EngineException if transaction is active
     */
    public void checkTransactionInactive(@Nonnull Statement statement) throws EngineException {
        Objects.requireNonNull(statement);
        if (sqlProcessor.isTransactionActive()) {
            throw new EngineException(MessageFormat.format("transaction is running (line={0}, column={1})", //
                    statement.getRegion().getStartLine() + 1, statement.getRegion().getStartColumn() + 1));
        }
    }
}
