package com.tsurugidb.console.cli.config;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;

import com.tsurugidb.console.cli.argument.ScriptArgument;
import com.tsurugidb.console.core.config.ScriptCommitMode;

/**
 * ConfigBuilder for script
 */
public class ScriptConfigBuilder extends ConfigBuilder<ScriptArgument> {

    private Charset encoding;
    private Path script;

    public ScriptConfigBuilder(ScriptArgument argument) {
        super(argument);
    }

    @Override
    protected void buildSub() {
        fillCommitMode(//
                argument.getAutoCommit(), argument.getNoAutoCommit(), //
                argument.getCommit(), argument.getNoCommit(), //
                ScriptCommitMode.COMMIT, //
                () -> List.of("--auto-commit", "--no-auto-commit", "--commit", "--no-commit").toString());

        fillEncoding();
        fillScript();
    }

    private void fillEncoding() {
        try {
            this.encoding = Charset.forName(argument.getEncoding());
        } catch (Exception e) {
            throw new RuntimeException("invalid encoding", e);
        }
        log.debug("config.encoding={}", encoding);
    }

    @Nonnull
    public Charset getEncoding() {
        return this.encoding;
    }

    private void fillScript() {
        try {
            this.script = Path.of(argument.getScript());
            if (!Files.exists(script)) {
                throw new FileNotFoundException(script.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("invalid script", e);
        }
        log.debug("config.script={}", script);
    }

    @Nonnull
    public Path getScript() {
        return this.script;
    }
}
