package com.tsurugidb.console.cli.config;

import java.util.EnumSet;

import com.tsurugidb.console.cli.argument.CliArgument;
import com.tsurugidb.console.core.config.ScriptClientVariableMap;
import com.tsurugidb.console.core.config.ScriptCommitMode;
import com.tsurugidb.console.core.config.ScriptCvKey;

/**
 * ConfigBuilder for console.
 */
public class ConsoleConfigBuilder extends ConfigBuilder {

    /**
     * Creates a new instance.
     *
     * @param argument argument
     */
    public ConsoleConfigBuilder(CliArgument argument) {
        super(argument);
    }

    @Override
    protected void fillClientVariableDefault(ScriptClientVariableMap clientVariableMap) {
        clientVariableMap.put(ScriptCvKey.SELECT_MAX_LINES, 1000);
        clientVariableMap.put(ScriptCvKey.TIMING, true);
    }

    @Override
    protected void buildSub() {
        fillCommitMode(EnumSet.of(ScriptCommitMode.AUTO_COMMIT, ScriptCommitMode.NO_AUTO_COMMIT), //
                ScriptCommitMode.NO_AUTO_COMMIT);
    }
}
