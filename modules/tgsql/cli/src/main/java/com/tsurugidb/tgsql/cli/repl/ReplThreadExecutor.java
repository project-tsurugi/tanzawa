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
package com.tsurugidb.tgsql.cli.repl;

import java.io.Closeable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jline.terminal.Terminal;
import org.jline.terminal.Terminal.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.exception.TgsqlMessageException;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * thread executor.
 */
public class ReplThreadExecutor implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(ReplThreadExecutor.class);

    private final String name;
    private final Terminal terminal;
    private final ExecutorService service = Executors.newCachedThreadPool();

    /**
     * Creates a new instance.
     *
     * @param name     executor name
     * @param terminal JLine Terminal
     */
    public ReplThreadExecutor(String name, Terminal terminal) {
        this.name = name;
        this.terminal = terminal;
    }

    /**
     * thread action.
     */
    @FunctionalInterface
    public interface EngineAction {
        /**
         * do action.
         *
         * @throws EngineException      if error occurred in engine
         * @throws ServerException      if server side error was occurred
         * @throws IOException          if I/O error was occurred
         * @throws InterruptedException if interrupted
         */
        void run() throws EngineException, ServerException, IOException, InterruptedException;
    }

    /**
     * invoke action.
     *
     * @param task action
     * @throws EngineException      if error occurred in engine
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred
     * @throws InterruptedException if interrupted
     */
    public void invoke(EngineAction task) throws EngineException, ServerException, IOException, InterruptedException {
        invoke(() -> {
            task.run();
            return null;
        });
    }

    /**
     * thread task.
     *
     * @param <R> return type
     */
    @FunctionalInterface
    public interface EngineTask<R> extends Callable<R> {
        @Override
        R call() throws EngineException, ServerException, IOException, InterruptedException;
    }

    /**
     * invoke task.
     *
     * @param <R>  return type
     * @param task task
     * @return return value from task
     * @throws EngineException      if error occurred in engine
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred
     * @throws InterruptedException if interrupted
     */
    public <R> R invoke(EngineTask<R> task) throws EngineException, ServerException, IOException, InterruptedException {
        long timingStart = System.nanoTime();
        var future = service.submit(task);
        var prevHandler = terminal.handle(Signal.INT, signal -> { // Ctrl+C
            LOG.trace("{} catch Signal.{}", name, signal);
            future.cancel(true);
        });
        try {
            return future.get();
        } catch (CancellationException e) {
            long timingEnd = System.nanoTime();
            LOG.trace("{} user cancelled", name, e);
            long time = timingEnd - timingStart;
            throw new TgsqlMessageException(MessageFormat.format("{0} cancelled", name), e, time);
        } catch (ExecutionException e) {
            LOG.debug("{} invoke catch ExecutionException", name, e);
            var c = e.getCause();
            if (c instanceof EngineException) {
                throw (EngineException) c;
            }
            if (c instanceof ServerException) {
                throw (ServerException) c;
            }
            if (c instanceof IOException) {
                throw (IOException) c;
            }
            if (c instanceof InterruptedException) {
                throw (InterruptedException) c;
            }
            if (c instanceof RuntimeException) {
                throw (RuntimeException) c;
            }
            throw new RuntimeException(c);
        } finally {
            terminal.handle(Signal.INT, prevHandler);
        }
    }

    @Override
    public void close() throws IOException {
        service.shutdownNow();
    }
}
