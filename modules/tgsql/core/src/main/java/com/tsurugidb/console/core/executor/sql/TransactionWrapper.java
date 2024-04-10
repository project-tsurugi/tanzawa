package com.tsurugidb.console.core.executor.sql;

import java.io.IOException;

import com.tsurugidb.sql.proto.SqlRequest;
import com.tsurugidb.sql.proto.SqlRequest.TransactionOption;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.Transaction;

/**
 * Transaction with transaction option.
 */
public class TransactionWrapper implements AutoCloseable {

    private final Transaction transaction;
    private final TransactionOption option;

    /**
     * Creates a new instance.
     *
     * @param transaction transaction
     * @param option      transaction option
     */
    public TransactionWrapper(Transaction transaction, SqlRequest.TransactionOption option) {
        this.transaction = transaction;
        this.option = option;
    }

    /**
     * get transaction.
     *
     * @return transaction
     */
    public Transaction getTransaction() {
        return this.transaction;
    }

    /**
     * get transaction option.
     *
     * @return transaction option
     */
    public SqlRequest.TransactionOption getOption() {
        return this.option;
    }

    @Override
    public void close() throws ServerException, IOException, InterruptedException {
        transaction.close();
    }
}
