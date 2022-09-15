package com.tsurugidb.console.cli.argument;

import java.nio.charset.Charset;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 * Argument for script.
 */
@Parameters(commandDescription = "execute SQL script file")
public class ScriptArgument extends CommonArgument {

    @Parameter(names = { "--encoding", "-e" }, arity = 1, description = "charset encoding")
    private String encoding = Charset.defaultCharset().name();

    @Parameter(names = { "--auto-commit" }, arity = 0, description = "commit every statement")
    private Boolean autoCommit;

    @Parameter(names = { "--no-auto-commit" }, arity = 0, description = "commit only if you explicitly specify a COMMIT statement")
    private Boolean noAutoCommit;

    @Parameter(names = { "--commit" }, arity = 0, description = "commit on success, rollback on failure (default)")
    private Boolean commit;

    @Parameter(names = { "--no-commit" }, arity = 0, description = "always rollback")
    private Boolean noCommit;

    @Parameter(description = "</path/to/script.sql>", required = true)
    private List<String> scriptList;

    /**
     * get --encoding.
     * 
     * @return encoding
     */
    @Nonnull
    public String getEncoding() {
        return this.encoding;
    }

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
     * get script.
     * 
     * @return script file path
     */
    @Nullable
    public String getScript() {
        assert this.scriptList != null;
        assert !this.scriptList.isEmpty();
        return scriptList.get(0);
    }
}
