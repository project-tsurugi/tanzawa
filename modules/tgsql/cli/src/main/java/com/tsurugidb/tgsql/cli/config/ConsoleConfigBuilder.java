package com.tsurugidb.tgsql.cli.config;

import java.util.EnumSet;

import com.tsurugidb.tgsql.cli.argument.CliArgument;
import com.tsurugidb.tgsql.core.config.ScriptClientVariableMap;
import com.tsurugidb.tgsql.core.config.ScriptCommitMode;
import com.tsurugidb.tgsql.core.config.ScriptCvKey;

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
        clientVariableMap.put(ScriptCvKey.SQL_TIMING, true);
    }

    @Override
    protected void buildSub() {
        fillCommitMode(EnumSet.of(ScriptCommitMode.AUTO_COMMIT, ScriptCommitMode.NO_AUTO_COMMIT), //
                ScriptCommitMode.NO_AUTO_COMMIT);
    }
}
