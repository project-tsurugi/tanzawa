package com.tsurugidb.console.cli.config;

import java.util.List;

import com.tsurugidb.console.cli.argument.ConsoleArgument;
import com.tsurugidb.console.core.config.ScriptClientVariableMap;
import com.tsurugidb.console.core.config.ScriptCommitMode;
import com.tsurugidb.console.core.config.ScriptCvKey;

/**
 * ConfigBuilder for console.
 */
public class ConsoleConfigBuilder extends ConfigBuilder<ConsoleArgument> {

    /**
     * Creates a new instance.
     *
     * @param argument argument
     */
    public ConsoleConfigBuilder(ConsoleArgument argument) {
        super(argument);
    }

    @Override
    protected void fillClientVariableDefault(ScriptClientVariableMap clientVariableMap) {
        clientVariableMap.put(ScriptCvKey.SELECT_MAX_LINES, 1000);
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
