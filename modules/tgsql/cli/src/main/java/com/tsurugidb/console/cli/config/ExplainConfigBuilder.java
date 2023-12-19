package com.tsurugidb.console.cli.config;

import com.tsurugidb.console.cli.argument.CliArgument;

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
