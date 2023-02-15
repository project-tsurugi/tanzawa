package com.tsurugidb.console.cli.argument;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.tsurugidb.console.cli.explain.ExplainConvertRunner;

/**
 * Argument for {@link ExplainConvertRunner}.
 */
@Parameters(commandDescription = "print explain", hidden = true)
public class ExplainArgument {

    @Parameter(names = { "--input", "-i" }, arity = 1, description = "explain json file", required = true)
    private String inputFile;

    @Parameter(names = { "--report", "-r" }, arity = 0, description = "report to stdout")
    private Boolean report;

    @Parameter(names = { "--output", "-o" }, arity = 1, description = "output file (dot)")
    private String outputFile = null;

    @Parameter(names = { "--verbose", "-v" }, arity = 0, description = "verbose")
    private Boolean verbose;

    @DynamicParameter(names = { "-D" }, description = "client variable. <key>=<value>")
    private Map<String, String> clientVariableMap = new LinkedHashMap<>();

    /**
     * get --input.
     *
     * @return explain json file path
     */
    public @Nonnull String getInputFile() {
        return this.inputFile;
    }

    /**
     * get --report.
     *
     * @return {@code true} if report
     */
    public boolean isReport() {
        return (this.report != null) ? this.report : false;
    }

    /**
     * get --output.
     *
     * @return output file path
     */
    public @Nullable String getOutputFile() {
        return this.outputFile;
    }

    /**
     * get --verbose.
     *
     * @return {@code true} if verbose
     */
    public boolean isVerbose() {
        return (this.verbose != null) ? this.verbose : false;
    }

    /**
     * get -D.
     *
     * @return client variable
     */
    public @Nonnull Map<String, String> getClientVariableMap() {
        return this.clientVariableMap;
    }
}
