package com.tsurugidb.console.cli.argument;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "SQL console")
public class ConsoleArgument extends CommonArgument {

    @Parameter(names = { "--auto-commit" }, arity = 0, description = "commit every statement")
    private Boolean autoCommit;

    @Parameter(names = { "--no-auto-commit" }, arity = 0, description = "commit only if you explicitly specify a COMMIT statement (default)")
    private Boolean noAutoCommit;

    public boolean isAutoCommit() {
        if (this.autoCommit == null && this.noAutoCommit == null) {
            return false; // default
        }
        if (this.autoCommit == null) {
            return !(this.noAutoCommit != null && this.noAutoCommit);
        }
        if (this.noAutoCommit == null) {
            return /* this.autoCommit != null && */ this.autoCommit;
        }

        assert this.autoCommit == true;
        assert this.noAutoCommit == true;
        throw new ParameterException("specify either --auto-commit or --no-auto-commit");
    }
}
