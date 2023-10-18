package com.tsurugidb.console.core.executor.engine;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.config.ScriptCommitMode;
import com.tsurugidb.console.core.config.ScriptConfig;
import com.tsurugidb.console.core.config.ScriptCvKey;
import com.tsurugidb.console.core.exception.ScriptNoMessageException;
import com.tsurugidb.console.core.executor.engine.command.SpecialCommand;
import com.tsurugidb.console.core.executor.explain.DotOutputHandler;
import com.tsurugidb.console.core.executor.explain.OptionHandler;
import com.tsurugidb.console.core.executor.explain.PlanGraphOutputHandler;
import com.tsurugidb.console.core.executor.explain.StatementMetadataHandler;
import com.tsurugidb.console.core.executor.report.ScriptReporter;
import com.tsurugidb.console.core.executor.result.ResultProcessor;
import com.tsurugidb.console.core.executor.sql.SqlProcessor;
import com.tsurugidb.console.core.model.CallStatement;
import com.tsurugidb.console.core.model.CommitStatement;
import com.tsurugidb.console.core.model.ErroneousStatement;
import com.tsurugidb.console.core.model.ErroneousStatement.ErrorKind;
import com.tsurugidb.console.core.model.ExplainStatement;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.console.core.model.StartTransactionStatement;
import com.tsurugidb.console.core.model.Statement;
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

    @Override
    public ScriptConfig getConfig() {
        return this.config;
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
    public void connect() throws ServerException, IOException, InterruptedException {
        sqlProcessor.connect(config);
    }

    @Override
    public boolean disconnect() throws ServerException, IOException, InterruptedException {
        return sqlProcessor.disconnect();
    }

    @Override
    public boolean executeEmptyStatement(@Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$
        return true;
    }

    @SuppressFBWarnings(value = "RCN", justification = "misdetection: SqlProcessor.execute() may return null")
    @Override
    public boolean executeGenericStatement(@Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$

        boolean transactionSatrtedImplicitly = checkTransactionActive(statement, true);
        try {
            executeTiming(timingEnd -> {
                try (var rs = sqlProcessor.execute(statement.getText(), statement.getRegion())) {
                    if (rs != null) {
                        timingEnd.accept(resultSetProcessor.process(rs));
                    } else {
                        timingEnd.accept(System.nanoTime());
                        reporter.reportStatementResult();
                    }
                }
            });
        } catch (Exception e) {
            if (transactionSatrtedImplicitly || config.getCommitMode() == ScriptCommitMode.AUTO_COMMIT) {
                try {
                    executeRollbackImplicitly();
                } catch (Exception e1) {
                    e.addSuppressed(e1);
                }
            }
            throw e;
        }

        if (transactionSatrtedImplicitly || config.getCommitMode() == ScriptCommitMode.AUTO_COMMIT) {
            executeCommitImplicitly();
        }
        return true;
    }

    @Override
    public boolean executeStartTransactionStatement(@Nonnull StartTransactionStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$

        checkTransactionInactive(statement);
        var option = ExecutorUtil.toTransactionOption(statement, config);
        executeTiming(timingEnd -> {
            sqlProcessor.startTransaction(option);
            timingEnd.accept(System.nanoTime());
            reporter.reportTransactionStarted(option);
        });
        return true;
    }

    @Override
    public boolean executeCommitStatement(@Nonnull CommitStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$

        checkTransactionActive(statement, false);
        var status = ExecutorUtil.toCommitStatus(statement);
        try {
            executeTiming(timingEnd -> {
                sqlProcessor.commitTransaction(status.orElse(null));
                timingEnd.accept(System.nanoTime());
                reporter.reportTransactionCommitted(status);
            });
        } catch (Throwable e) {
            reporter.reportTransactionCloseImplicitly();
            throw e;
        }
        return true;
    }

    /**
     * Executes {@code commit} operation implicitly.
     *
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    protected void executeCommitImplicitly() throws ServerException, IOException, InterruptedException {
        var status = config.getCommitStatus();
        try {
            executeTiming(timingEnd -> {
                sqlProcessor.commitTransaction(status);
                timingEnd.accept(System.nanoTime());
                reporter.reportTransactionCommittedImplicitly(status);
            });
        } catch (Throwable e) {
            reporter.reportTransactionCloseImplicitly();
            throw e;
        }
    }

    @Override
    public boolean executeRollbackStatement(@Nonnull Statement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$

        checkTransactionActive(statement, false);
        try {
            executeTiming(timingEnd -> {
                sqlProcessor.rollbackTransaction();
                timingEnd.accept(System.nanoTime());
                reporter.reportTransactionRollbacked();
            });
        } catch (Throwable e) {
            reporter.reportTransactionCloseImplicitly();
            throw e;
        }
        return true;
    }

    /**
     * Executes {@code rollback} operation implicitly.
     *
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while executing the statement
     * @throws InterruptedException if interrupted while executing the statement
     */
    protected void executeRollbackImplicitly() throws ServerException, IOException, InterruptedException {
        try {
            executeTiming(timingEnd -> {
                sqlProcessor.rollbackTransaction();
                timingEnd.accept(System.nanoTime());
                reporter.reportTransactionRollbackedImplicitly();
            });
        } catch (Throwable e) {
            reporter.reportTransactionCloseImplicitly();
            throw e;
        }
    }

    @Override
    public boolean executeCallStatement(@Nonnull CallStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        // fall-back
        return executeGenericStatement(statement);
    }

    @Override
    public boolean executeExplainStatement(@Nonnull ExplainStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        Objects.requireNonNull(statement);
        LOG.debug("execute: kind={}, text={}", statement.getKind(), statement.getText()); //$NON-NLS-1$

        // FIXME: enable to configure command path
        var path = CommandPath.system();
        StatementMetadataHandler metadataHandler;
        var outputHandlers = new ArrayList<PlanGraphOutputHandler>();
        try {
            metadataHandler = StatementMetadataHandler.fromOptions(statement.getOptions());
            outputHandlers.add((rep, graph) -> {
                rep.reportExecutionPlan(statement.getBody().getText(), graph);
            });
            var options = DotOutputHandler.extendOptions(statement.getOptions(), config.getClientVariableMap());
            outputHandlers.add(DotOutputHandler.fromOptions(options, path));
        } catch (EngineConfigurationException e) {
            LOG.debug("error occurred while handling explain options", e); //$NON-NLS-1$
            return execute(new ErroneousStatement(statement.getText(), statement.getRegion(), e.getErrorKind(), e.getOccurrence(), e.getMessage()));
        }
        var handlers = new ArrayList<OptionHandler>();
        handlers.add(metadataHandler);
        handlers.addAll(outputHandlers);

        for (var entry : statement.getOptions().entrySet()) {
            var key = entry.getKey().getValue();
            if (handlers.stream().anyMatch(handler -> handler.isHandled(key))) {
                continue;
            }
            return execute(new ErroneousStatement(statement.getText(), statement.getRegion(), ErrorKind.UNKNOWN_EXPLAIN_OPTION, entry.getKey().getRegion(),
                    MessageFormat.format("unrecognized explain option: \"{0}\"", key)));
        }

        executeTiming(timingEnd -> {
            var metadata = sqlProcessor.explain(statement.getBody().getText(), statement.getBody().getRegion());
            LOG.debug("explain format: id={}, version={}", metadata.getFormatId(), metadata.getFormatVersion()); //$NON-NLS-1$
            LOG.trace("explain contents: {}", metadata.getContents()); //$NON-NLS-1$

            var graph = metadataHandler.handle(reporter, metadata);
            for (var output : outputHandlers) {
                output.handle(reporter, graph);
            }
            timingEnd.accept(System.nanoTime());
        });
        return true;
    }

    @Override
    public boolean executeSpecialStatement(@Nonnull SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
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
    public boolean executeErroneousStatement(@Nonnull ErroneousStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
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

    // @return {@code true} if transaction started implicitly
    private boolean checkTransactionActive(Statement statement, boolean startIfInactive) throws EngineException, ServerException, IOException, InterruptedException {
        if (sqlProcessor.isTransactionActive()) {
            return false;
        }

        if (startIfInactive) {
            var option = config.getTransactionOption();
            if (option != null) {
                reporter.reportStartTransactionImplicitly(option);
                executeTiming(timingEnd -> {
                    sqlProcessor.startTransaction(option);
                    timingEnd.accept(System.nanoTime());
                });
                return true;
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

    private interface TimingTask {
        void run(LongConsumer timingEnd) throws Exception;
    }

    private void executeTiming(TimingTask task) {
        long timingStart = System.nanoTime();
        class TimingEnd implements LongConsumer {
            long time = timingStart;

            @Override
            public void accept(long t) {
                this.time = t;
            }
        }
        var timingEnd = new TimingEnd();
        try {
            task.run(timingEnd);
        } catch (InterruptedException e) {
            // thread interrupted
            return;
        } catch (ServerException e) {
            reporter.warn(e);
            reportTiming(timingStart, timingEnd.time);
            throw new ScriptNoMessageException(e);
        } catch (Exception e) {
            for (var c = e.getCause(); c != null; c = c.getCause()) {
                if (c instanceof InterruptedException) {
                    // thread interrupted
                    return;
                }
            }
            String message = e.getMessage();
            if (message == null) {
                message = e.getClass().getName();
            }
            reporter.warn(message);
            reportTiming(timingStart, timingEnd.time);
            throw new ScriptNoMessageException(e);
        }
        reportTiming(timingStart, timingEnd.time);
    }

    private void reportTiming(long timingStart, long timingEnd) {
        if (timingEnd == timingStart) {
            timingEnd = System.nanoTime();
        }

        var clientVariableMap = config.getClientVariableMap();
        boolean timing = clientVariableMap.get(ScriptCvKey.SQL_TIMING, false);
        if (timing) {
            reporter.reportTiming(timingEnd - timingStart);
        }
    }
}
