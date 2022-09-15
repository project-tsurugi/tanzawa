package com.tsurugidb.console.cli.argument;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Argument for exec.
 */
@Parameters(commandDescription = "execute a SQL statement")
public class ExecArgument extends CommonArgument {

    @Parameter(names = { "--commit" }, arity = 0, description = "commit on success, rollback on failure")
    private Boolean commit;

    @Parameter(names = { "--no-commit" }, arity = 0, description = "always rollback")
    private Boolean noCommit;

    @Parameter(description = "<statement>", required = true)
    private List<String> statementList;

    /**
     * get --commit.
     * 
     * @return commit
     */
    @Nullable
    public Boolean getCommit() {
        return this.commit;
    }

    /**
     * get --no-commit.
     * 
     * @return no commit
     */
    @Nullable
    public Boolean getNoCommit() {
        return this.noCommit;
    }

    /**
     * get SQL statement.
     * 
     * @return statement
     */
    @Nonnull
    public String getStatement() {
        assert this.statementList != null;
        return String.join(" ", statementList);
    }
}
