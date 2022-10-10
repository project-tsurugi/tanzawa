package com.tsurugidb.console.core.executor.explain;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.executor.engine.CommandPath;
import com.tsurugidb.console.core.executor.engine.EngineConfigurationException;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.executor.report.ScriptReporter;
import com.tsurugidb.console.core.model.ErroneousStatement;
import com.tsurugidb.console.core.model.Regioned;
import com.tsurugidb.console.core.model.Value;
import com.tsurugidb.tsubakuro.explain.DotGenerator;
import com.tsurugidb.tsubakuro.explain.PlanGraph;

/**
 * Outputs execution plan using Graphviz DOT.
 */
public class DotOutputHandler implements PlanGraphOutputHandler {

    /**
     * The explain option name prefix of this handler.
     */
    public static final String KEY_PREFIX = "dot."; //$NON-NLS-1$

    /**
     * The explain option name of enabling verbose execution plan.
     */
    public static final String KEY_VERBOSE = KEY_PREFIX + "verbose"; //$NON-NLS-1$

    /**
     * The explain option name of output target path.
     */
    public static final String KEY_OUTPUT = KEY_PREFIX + "output"; //$NON-NLS-1$

    /**
     * The explain option name of the DOT executable path.
     */
    public static final String KEY_EXECUTABLE = KEY_PREFIX + "executable"; //$NON-NLS-1$

    /**
     * The DOT command name.
     */
    public static final String DOT_COMMAND = "dot"; //$NON-NLS-1$

    /**
     * The extension name of DOT script files.
     */
    public static final String EXT_DOT = "dot"; //$NON-NLS-1$

    static final Logger LOG = LoggerFactory.getLogger(DotOutputHandler.class);

    private static final PlanGraphOutputHandler NULL_OUTPUT_HANDLER = new PlanGraphOutputHandler() {

        @Override
        public void handle(ScriptReporter reporter, PlanGraph graph) {
            return;
        }

        @Override
        public boolean isHandled(String key) {
            return isHandled0(key);
        }
    };

    private final @Nonnull DotGenerator generator;

    private final @Nonnull Path output;

    private final @Nullable Path dotExecutable;

    /**
     * Creates a new instance.
     * @param generator the DOT script generator
     * @param output the plan graph output.
     * @param dotExecutable the DOT executable file
     */
    public DotOutputHandler(@Nonnull DotGenerator generator, @Nonnull Path output, @Nullable Path dotExecutable) {
        Objects.requireNonNull(generator);
        Objects.requireNonNull(output);
        this.generator = generator;
        this.output = output;
        this.dotExecutable = dotExecutable;
    }

    /**
     * Creates a new instance from {@code EXPLAIN} statement options.
     * @param options the explain statement options
     * @param path the command path
     * @return the created instance
     * @throws EngineConfigurationException if options are invalid
     */
    public static PlanGraphOutputHandler fromOptions(
            @Nonnull Map<Regioned<String>, Optional<Regioned<Value>>> options,
            @Nonnull CommandPath path) throws EngineConfigurationException {
        Objects.requireNonNull(options);
        Objects.requireNonNull(path);
        var output = Util.findPath(options, KEY_OUTPUT).orElse(null);
        if (output == null) {
            LOG.trace("DotOutputHandler is disabled"); //$NON-NLS-1$
            return NULL_OUTPUT_HANDLER;
        }
        if (Files.isDirectory(output)) {
            throw new EngineConfigurationException(
                    ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION,
                    Util.findValue(options, KEY_OUTPUT).map(Regioned::getRegion).get(),
                    MessageFormat.format(
                            "output is already exists as a directory: {0}",
                            output));
        }
        LOG.trace("DotOutputHandler is enabled: output={}", output); //$NON-NLS-1$
        var generator = DotGenerator.newBuilder();
        if (Util.findBoolean(options, KEY_VERBOSE).orElse(false)) {
            generator.withShowNodeKind(true);
        }
        var requireCommand = findExtension(output)
                .map(it -> !it.equals(EXT_DOT))
                .orElse(false);
        var executable = Util.findPath(options, KEY_EXECUTABLE).orElse(null);
        if (executable != null && !Files.isExecutable(executable)) {
            throw new EngineConfigurationException(
                    ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION,
                    Util.findValue(options, KEY_OUTPUT).map(Regioned::getRegion).get(),
                    MessageFormat.format(
                            "\"{0}\" command is not executable: {1}",
                            DOT_COMMAND,
                            output));
        }
        if (executable == null && requireCommand) {
            executable = path.find(DOT_COMMAND)
                    .orElseThrow(() -> new EngineConfigurationException(
                            ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION,
                            Util.findKey(options, KEY_OUTPUT).map(Regioned::getRegion).get(),
                            MessageFormat.format(
                                    "\"{0}\" is not found in command path, please specify \"{1}\" option",
                                    DOT_COMMAND,
                                    KEY_EXECUTABLE)));
        }
        return new DotOutputHandler(generator.build(), output, executable);
    }

    @Override
    public boolean isHandled(@Nonnull String key) {
        return isHandled0(key);
    }

    private static boolean isHandled0(@Nonnull String key) {
        Objects.requireNonNull(key);
        return key.equalsIgnoreCase(KEY_VERBOSE)
                || key.equalsIgnoreCase(KEY_OUTPUT)
                || key.equals(KEY_EXECUTABLE);
    }

    @Override
    public void handle(@Nonnull ScriptReporter reporter, @Nonnull PlanGraph graph)
            throws EngineException, InterruptedException {
        Objects.requireNonNull(reporter);
        Objects.requireNonNull(graph);
        LOG.trace("generating DOT script"); //$NON-NLS-1$
        var buf = new StringBuilder();
        try {
            generator.write(graph, buf);
        } catch (IOException e) {
            // may not occur
            throw new EngineException("error occurred while generating DOT script", e);
        }
        LOG.trace("DOT script : {}", buf); //$NON-NLS-1$
        try {
            dump(reporter, buf);
        } catch (IOException e) {
            throw new EngineException(MessageFormat.format(
                    "failed to output explain result: {0}",
                    output), e);
        }
    }

    private void dump(ScriptReporter reporter, StringBuilder buf) throws IOException, InterruptedException {
        var extension = findExtension(output).orElse(null);
        Util.prepareParentDirectory(output);
        if (dotExecutable == null || extension == null) {
            LOG.debug("printing raw DOT script: {}", output); //$NON-NLS-1$
            Files.writeString(output, buf, StandardCharsets.UTF_8);
            reporter.info(MessageFormat.format(
                    "saved DOT script: {0}",
                    output));
            return;
        }
        LOG.debug("executing DOT: exec={}, output={}", dotExecutable, output); //$NON-NLS-1$
        var commandLine = List.of(
                dotExecutable.toString(),
                String.format("-T%s", extension.toLowerCase(Locale.ENGLISH)), //$NON-NLS-1$
                "-Grankdir=RL",
                "-Nshape=rect");
        LOG.debug("execute: {}", commandLine);
        var process = new ProcessBuilder(commandLine)
                .redirectOutput(Redirect.to(output.toFile()))
                .redirectInput(Redirect.PIPE)
                .start();
        try (
            var stdin = process.getOutputStream();
            var writer = new OutputStreamWriter(stdin, StandardCharsets.UTF_8);
        ) {
            writer.append(buf);
            writer.close();
            var exit = process.waitFor();
            if (exit == 0) {
                reporter.info(MessageFormat.format(
                        "saved execution plan graph: {0}",
                        output));
            } else {
                reporter.warn(MessageFormat.format(
                        "DOT command returned by exit status = {0}",
                        exit));
                return;
            }
        } finally {
            process.destroyForcibly();
        }
    }

    private static Optional<String> findExtension(Path path) {
        var name = Optional.ofNullable(path.getFileName())
                .map(Path::toString)
                .orElse(null);
        if (name == null) {
            return Optional.empty();
        }
        var extensionAt = name.lastIndexOf('.');
        if (extensionAt >= 0) {
            return Optional.of(name.substring(extensionAt + 1));
        }
        return Optional.empty();
    }
}
