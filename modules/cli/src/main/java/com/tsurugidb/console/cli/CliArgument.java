package com.tsurugidb.console.cli;

import javax.annotation.Nullable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(separators = "=")
public class CliArgument {

    @Parameter(names = { "-?", "--help" }, description = "print usage", help = true)
    private boolean help;

    @Parameter(names = { "-e", "--endpoint" }, description = "connection uri (e.g. tcp://localhost:12345)", required = true, arity = 1)
    private String endpoint;

    @Parameter(names = { "-f", "--file" }, description = "script file(UTF-8) (/path/to/script.sql or '-')", arity = 1)
    private String scriptFile = "-";

    public boolean isHelp() {
        return this.help;
    }

    @Nullable
    public String getEndpoint() {
        return this.endpoint;
    }

    @Nullable
    public String getScriptFile() {
        return this.scriptFile;
    }

    public boolean isStdin() {
        String file = getScriptFile();
        return (file == null) || file.trim().equals("-");
    }
}
