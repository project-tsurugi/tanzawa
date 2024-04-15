package com.tsurugidb.tgsql.cli.config;

import java.util.EnumSet;

import javax.annotation.Nonnull;

import com.tsurugidb.tgsql.cli.argument.CliArgument;
import com.tsurugidb.tgsql.core.config.TgsqlCommitMode;

/**
 * ConfigBuilder for exec.
 */
public class ExecConfigBuilder extends ConfigBuilder {

    private String statement;

    /**
     * Creates a new instance.
     *
     * @param argument argument
     */
    public ExecConfigBuilder(CliArgument argument) {
        super(argument);
    }

    @Override
    protected void buildSub() {
        fillCommitMode(EnumSet.of(TgsqlCommitMode.COMMIT, TgsqlCommitMode.NO_COMMIT), //
                TgsqlCommitMode.COMMIT);

        fillStatement();
    }

    private void fillStatement() {
        this.statement = argument.getStatement();
        log.debug("config.statement=[{}]", statement);
    }

    /**
     * get SQL statement.
     *
     * @return statement
     */
    @Nonnull
    public String getStatement() {
        return this.statement;
    }
}
