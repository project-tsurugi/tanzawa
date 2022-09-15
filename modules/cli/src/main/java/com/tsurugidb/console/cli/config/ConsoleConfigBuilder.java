package com.tsurugidb.console.cli.config;

import java.util.List;

import com.tsurugidb.console.cli.argument.ConsoleArgument;
import com.tsurugidb.console.core.config.ScriptCommitMode;

/**
 * ConfigBuilder for console
 */
public class ConsoleConfigBuilder extends ConfigBuilder<ConsoleArgument> {

    public ConsoleConfigBuilder(ConsoleArgument argument) {
        super(argument);
    }

    @Override
    protected void buildSub() {
        fillCommitMode(//
                argument.getAutoCommit(), argument.getNoAutoCommit(), //
                null, null, //
                ScriptCommitMode.NO_AUTO_COMMIT, //
                () -> List.of("--auto-commit", "--no-auto-commit").toString());
    }
}
