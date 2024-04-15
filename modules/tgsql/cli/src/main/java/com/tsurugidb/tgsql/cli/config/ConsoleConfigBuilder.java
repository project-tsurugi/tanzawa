package com.tsurugidb.tgsql.cli.config;

import java.util.EnumSet;

import com.tsurugidb.tgsql.cli.argument.CliArgument;
import com.tsurugidb.tgsql.core.config.TgsqlClientVariableMap;
import com.tsurugidb.tgsql.core.config.TgsqlCommitMode;
import com.tsurugidb.tgsql.core.config.TgsqlCvKey;

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
    protected void fillClientVariableDefault(TgsqlClientVariableMap clientVariableMap) {
        clientVariableMap.put(TgsqlCvKey.SELECT_MAX_LINES, 1000);
        clientVariableMap.put(TgsqlCvKey.SQL_TIMING, true);
    }

    @Override
    protected void buildSub() {
        fillCommitMode(EnumSet.of(TgsqlCommitMode.AUTO_COMMIT, TgsqlCommitMode.NO_AUTO_COMMIT), //
                TgsqlCommitMode.NO_AUTO_COMMIT);
    }
}
