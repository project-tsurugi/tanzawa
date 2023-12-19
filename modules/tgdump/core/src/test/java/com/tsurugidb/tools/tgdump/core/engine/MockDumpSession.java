package com.tsurugidb.tools.tgdump.core.engine;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;
import com.tsurugidb.tsubakuro.sql.TableMetadata;

/**
 * Mock implementation of {@link DumpSession}.
 */
public class MockDumpSession implements DumpSession {

    /**
     * The session state.
     */
    public enum State {

        /**
         * Preparing the dump targets.
         */
        PREPARING,

        /**
         * Running the dump operations.
         */
        RUNNING,

        /**
         * Committed the dump operations.
         */
        COMMITTED,

        /**
         * Aborted or Not started the dump operations.
         */
        CLOSED,
    }

    private final AtomicReference<State> stateRef = new AtomicReference<>(State.PREPARING);

    private final Map<String, TableMetadata> expected;

    private final Map<String, TableMetadata> registered = new ConcurrentHashMap<>();

    private final Map<String, Path> executed = new ConcurrentHashMap<>();

    /**
     * Creates a new instance.
     * @param expected the expected table names
     */
    public MockDumpSession(String... expected) {
        this.expected = Arrays.stream(expected)
                .collect(Collectors.toMap(Function.identity(), MockTableMetadata::new));
    }

    /**
     * Creates a new instance.
     * @param expected the expected tables
     */
    public MockDumpSession(List<? extends TableMetadata> expected) {
        this.expected = expected.stream()
                .collect(Collectors.toMap(TableMetadata::getTableName, Function.identity()));
    }

    /**
     * Returns the current state.
     * @return the current state
     */
    public State getState() {
        return stateRef.get();
    }

    /**
     * Returns the registered tables.
     * @return the registered tables
     */
    public Map<String, TableMetadata> getRegistered() {
        return registered;
    }

    /**
     * Returns the executed tables.
     * @return the executed tables
     */
    public Map<String, Path> getExecuted() {
        return executed;
    }

    @Override
    public void register(DumpMonitor monitor, DumpTarget target)
            throws InterruptedException, DiagnosticException {
        if (stateRef.get() != State.PREPARING) {
            throw new IllegalStateException();
        }
        var found = expected.get(target.getTableName());
        if (found == null) {
            throw new DiagnosticException(DumpDiagnosticCode.UNKNOWN, List.of(target.getTableName()));
        }
        var prev = registered.putIfAbsent(target.getTableName(), found);
        if (prev == null) {
            monitor.onDumpInfo(target.getTableName(), found, target.getDestination());
        }
    }

    @Override
    public void begin(DumpMonitor monitor) throws InterruptedException, DiagnosticException {
        if (!stateRef.compareAndSet(State.PREPARING, State.RUNNING)) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void execute(DumpMonitor monitor, DumpTarget target) throws InterruptedException, DiagnosticException {
        if (stateRef.get() != State.RUNNING) {
            throw new IllegalStateException();
        }
        if (!registered.containsKey(target.getTableName())) {
            throw new IllegalArgumentException();
        }
        if (executed.containsKey(target.getTableName())) {
            throw new IllegalStateException();
        }
        executed.put(target.getTableName(), target.getDestination());
        monitor.onDumpFinish(target.getTableName(), target.getDestination());
    }

    @Override
    public void commit(DumpMonitor monitor) throws InterruptedException, DiagnosticException {
        if (!stateRef.compareAndSet(State.RUNNING, State.COMMITTED)) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void close() throws InterruptedException, DiagnosticException {
        stateRef.set(State.CLOSED);
    }
}
