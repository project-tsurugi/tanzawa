package com.tsurugidb.console.cli.config;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import javax.annotation.Nonnull;

import com.tsurugidb.console.cli.argument.CliArgument;
import com.tsurugidb.console.core.config.ScriptCommitMode;

/**
 * ConfigBuilder for script.
 */
public class ScriptConfigBuilder extends ConfigBuilder {

    private Charset encoding;
    private Path script;

    /**
     * Creates a new instance.
     *
     * @param argument argument
     */
    public ScriptConfigBuilder(CliArgument argument) {
        super(argument);
    }

    @Override
    protected void buildSub() {
        fillCommitMode(EnumSet.of(ScriptCommitMode.AUTO_COMMIT, ScriptCommitMode.NO_AUTO_COMMIT, ScriptCommitMode.COMMIT, ScriptCommitMode.NO_COMMIT), //
                ScriptCommitMode.COMMIT);

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

    /**
     * get script file encoding.
     *
     * @return encoding
     */
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

    /**
     * get script.
     *
     * @return script file path
     */
    @Nonnull
    public Path getScript() {
        return this.script;
    }
}
