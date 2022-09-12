package com.tsurugidb.console.cli.argument;

import java.util.List;

import javax.annotation.Nonnull;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "execute a SQL statement")
public class ExecArgument extends CommonArgument {

    @Parameter(names = { "--commit" }, arity = 0, description = "commit on success, rollback on failure")
    private Boolean commit;

    @Parameter(names = { "--no-commit" }, arity = 0, description = "always rollback")
    private Boolean noCommit;

    @Parameter(description = "<statement>", required = true)
    private List<String> statementList;

    public boolean isCommit() {
        if (this.commit == null && this.noCommit == null) {
            throw new ParameterException("specify --commit or --no-commit");
        }
        if (this.commit == null) {
            return !(this.noCommit != null && this.noCommit);
        }
        if (this.noCommit == null) {
            return /* this.commit != null && */ this.commit;
        }

        assert this.commit == true;
        assert this.noCommit == true;
        throw new ParameterException("specify either --commit or --no-commit");
    }

    @Nonnull
    public String getStatement() {
        assert this.statementList != null;
        return String.join(" ", statementList);
    }
}
