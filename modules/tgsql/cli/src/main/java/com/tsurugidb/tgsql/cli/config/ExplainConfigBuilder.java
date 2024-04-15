package com.tsurugidb.tgsql.cli.config;

import com.tsurugidb.tgsql.cli.argument.CliArgument;

/**
 * ConfigBuilder for explain.
 */
public class ExplainConfigBuilder extends ConfigBuilder {

    /**
     * Creates a new instance.
     *
     * @param argument argument
     */
    public ExplainConfigBuilder(CliArgument argument) {
        super(argument);
    }

    @Override
    protected void buildSub() {
        // do nothing
    }
}
