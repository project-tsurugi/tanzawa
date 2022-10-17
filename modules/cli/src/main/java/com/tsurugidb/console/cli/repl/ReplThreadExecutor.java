package com.tsurugidb.console.cli.repl;

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

import com.tsurugidb.console.core.exception.ScriptMessageException;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * thread executor.
 */
public class ReplThreadExecutor implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(ReplThreadExecutor.class);

    private final String name;
    private final Terminal terminal;
    private final ExecutorService service = Executors.newFixedThreadPool(4);

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
         * @throws EngineException
         * @throws ServerException
         * @throws IOException
         * @throws InterruptedException
         */
        void run() throws EngineException, ServerException, IOException, InterruptedException;
    }

    /**
     * invoke action.
     *
     * @param task action
     * @throws EngineException
     * @throws ServerException
     * @throws IOException
     * @throws InterruptedException
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
     * @throws EngineException
     * @throws ServerException
     * @throws IOException
     * @throws InterruptedException
     */
    public <R> R invoke(EngineTask<R> task) throws EngineException, ServerException, IOException, InterruptedException {
        var future = service.submit(task);
        var prevHandler = terminal.handle(Signal.INT, signal -> { // Ctrl+C
            LOG.trace("{} catch Signal.{}", name, signal);
            future.cancel(true);
        });
        try {
            return future.get();
        } catch (CancellationException e) {
            LOG.trace("{} user cancelled", name, e);
            throw new ScriptMessageException(MessageFormat.format("{0} cancelled", name), e);
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
