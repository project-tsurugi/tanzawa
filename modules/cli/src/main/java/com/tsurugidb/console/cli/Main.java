package com.tsurugidb.console.cli;

import java.net.URI;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.tsurugidb.console.cli.repl.ReplReader;
import com.tsurugidb.console.cli.repl.ReplReporter;
import com.tsurugidb.console.cli.repl.ReplLineReader;
import com.tsurugidb.console.cli.repl.ReplResultProcessor;
import com.tsurugidb.console.core.ScriptConfig;
import com.tsurugidb.console.core.ScriptRunner;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;

/**
 * A program entry of Tsurugi SQL console cli.
 */
public final class Main {

    /**
     * Executes a script file.
     * <ul>
     * <li>{@code --file} : path to the script file (UTF-8 encoded)</li>
     * <li>{@code --endpoint} : connection URI</li>
     * </ul>
     * 
     * @param args the program arguments
     * @throws Exception if exception was occurred
     */
    public static void main(String... args) throws Exception {
        var argument = new CliArgument();
        var commander = JCommander.newBuilder() //
                .programName("tgsql") //
                .addObject(argument) //
                .build();
        try {
            commander.parse(args);
        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            e.usage();
            System.exit(1);
        }

        if (argument.isHelp()) {
            commander.usage();
            return;
        }

        var config = createConfig(argument);

        if (argument.isStdin()) {
            var lineReader = ReplLineReader.create();
            var reporter = new ReplReporter(lineReader.getTerminal());
            try (var reader = new ReplReader(lineReader); //
                    var resultProcessor = new ReplResultProcessor(reporter)) {
                ScriptRunner.repl(config, reader, resultProcessor, reporter);
            }
            return;
        }

        ScriptRunner.execute(argument.getScriptFile(), config);
    }

    private static ScriptConfig createConfig(CliArgument argument) {
        var config = new ScriptConfig();

        var endpoint = URI.create(argument.getEndpoint());
        config.setEndpoint(endpoint);

        config.setCredential(NullCredential.INSTANCE);

        return config;
    }

    private Main() {
        throw new AssertionError();
    }
}
