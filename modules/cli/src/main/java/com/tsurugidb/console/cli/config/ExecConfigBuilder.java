package com.tsurugidb.console.cli.config;

import java.util.List;

import javax.annotation.Nonnull;

import com.tsurugidb.console.cli.argument.ExecArgument;
import com.tsurugidb.console.core.config.ScriptCommitMode;

/**
 * ConfigBuilder for exec
 */
public class ExecConfigBuilder extends ConfigBuilder<ExecArgument> {

    private String statement;

    public ExecConfigBuilder(ExecArgument argument) {
        super(argument);
    }

    @Override
    protected void buildSub() {
        fillCommitMode(//
                null, null, //
                argument.getCommit(), argument.getNoCommit(), //
                ScriptCommitMode.COMMIT, //
                () -> List.of("--commit", "--no-commit").toString());

        fillStatement();
    }

    private void fillStatement() {
        this.statement = argument.getStatement();
        log.debug("config.statement=[{}]", statement);
    }

    @Nonnull
    public String getStatement() {
        return this.statement;
    }
}
