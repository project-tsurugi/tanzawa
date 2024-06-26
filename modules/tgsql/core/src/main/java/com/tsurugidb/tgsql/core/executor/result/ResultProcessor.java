package com.tsurugidb.tgsql.core.executor.result;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.Nonnull;

import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.ResultSet;

/**
 * Processes {@link ResultSet}.
 */
@FunctionalInterface
public interface ResultProcessor extends Closeable {

    /**
     * Processes {@link ResultSet}.
     *
     * @param target the target result set
     * @return {@link System#nanoTime()}
     * @throws ServerException      if server side error was occurred
     * @throws IOException          if I/O error was occurred while processing the result set
     * @throws InterruptedException if interrupted while processing the result set
     */
    long process(@Nonnull ResultSet target) throws ServerException, IOException, InterruptedException;

    @Override
    default void close() throws IOException {
        return;
    }
}
