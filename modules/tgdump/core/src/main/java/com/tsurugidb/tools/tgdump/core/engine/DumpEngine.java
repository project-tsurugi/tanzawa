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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.monitoring.MonitoringException;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;

/**
 * Performs dump operations.
 */
public class DumpEngine {

    private static final Logger LOG = LoggerFactory.getLogger(DumpEngine.class);

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactory() {

        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            var result = new Thread(r);
            result.setName(String.format("TSURUGI-DUMP-WORKER-%d", counter.incrementAndGet())); //$NON-NLS-1$
            result.setDaemon(true);
            return result;
        }
    };

    private final int numberOfWorkers;

    /**
     * Creates a new instance with a single worker thread.
     */
    public DumpEngine() {
        this(1);
    }

    /**
     * Creates a new instance.
     * @param numberOfWorkers the number of worker threads
     */
    public DumpEngine(int numberOfWorkers) {
        if (numberOfWorkers < 0) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "the number of workers must be > 0 ({0})",
                    numberOfWorkers));
        }
        this.numberOfWorkers = numberOfWorkers;
    }

    /**
     * Executes a series of dump operations.
     * @param monitor the execution monitor
     * @param session the database session to execute dump operations
     * @param targets the dump targets information
     * @throws InterruptedException if interrupted during the operation
     * @throws DumpException if error occurred while executing dump operations
     * @throws MonitoringException if error occurred while providing monitoring information
     * @throws DiagnosticException if error occurred while the execution
     */
    public void execute(
            @Nonnull DumpMonitor monitor,
            @Nonnull DumpSession session,
            @Nonnull List<? extends DumpTarget> targets) throws InterruptedException, DiagnosticException {
        Objects.requireNonNull(monitor);
        Objects.requireNonNull(session);
        Objects.requireNonNull(targets);
        LOG.debug("creating dump operation worker: {}", numberOfWorkers); //$NON-NLS-1$
        var threads = Executors.newFixedThreadPool(numberOfWorkers, THREAD_FACTORY);
        try {
            monitor.verbose("preparing dump operations"); //$NON-NLS-1$
            // register the dump target tables
            var prepareJobs = targets.stream()
                    .map(target -> threads.submit(() -> doPrepare(monitor, session, target)))
                    .collect(Collectors.toList());
            waitForCompletion(threads, prepareJobs);

            session.begin(monitor);

            monitor.verbose("starting dump operations"); //$NON-NLS-1$
            var executeJobs = targets.stream()
                    .map(target -> threads.submit(() -> doExecute(monitor, session, target)))
                    .collect(Collectors.toList());
            waitForCompletion(threads, executeJobs);
            monitor.verbose("finishing dump operations"); //$NON-NLS-1$

            session.commit(monitor);
        } finally {
            threads.shutdown();
        }
        monitor.verbose("completed dump operations"); //$NON-NLS-1$
    }

    static Void doPrepare(DumpMonitor monitor, DumpSession session, DumpTarget target)
            throws InterruptedException, DiagnosticException {
        session.register(monitor, target);
        return null;
    }

    static Void doExecute(DumpMonitor monitor, DumpSession session, DumpTarget target)
            throws InterruptedException, DiagnosticException {
        session.execute(monitor, target);
        return null;
    }

    private static void waitForCompletion(ExecutorService threads, List<? extends Future<?>> jobs)
            throws InterruptedException, DiagnosticException {
        boolean green = false;
        try {
            for (var job : jobs) {
                try {
                    job.get();
                } catch (ExecutionException e) {
                    var cause = e.getCause();
                    if (cause instanceof DiagnosticException) {
                        throw (DiagnosticException) cause;
                    }
                    if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    throw new DiagnosticException(DumpDiagnosticCode.UNKNOWN, List.of(cause.toString()), cause);
                }
            }
            green = true;
        } finally {
            if (!green) {
                threads.shutdownNow();
            }
        }
    }
}
