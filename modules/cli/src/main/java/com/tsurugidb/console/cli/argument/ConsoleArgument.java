package com.tsurugidb.console.cli.argument;

import javax.annotation.Nullable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Argument for console.
 */
@Parameters(commandDescription = "SQL console")
public class ConsoleArgument extends CommonArgument {

    @Parameter(names = { "--auto-commit" }, arity = 0, description = "commit every statement")
    private Boolean autoCommit;

    @Parameter(names = { "--no-auto-commit" }, arity = 0, description = "commit only if you explicitly specify a COMMIT statement (default)")
    private Boolean noAutoCommit;

    /**
     * get --auto-commit.
     * 
     * @return auto commit
     */
    @Nullable
    public Boolean getAutoCommit() {
        return this.autoCommit;
    }

    /**
     * get --no-auto-commit.
     * 
     * @return no auto commit
     */
    @Nullable
    public Boolean getNoAutoCommit() {
        return this.noAutoCommit;
    }
}
