package com.tsurugidb.tgsql.core.executor.explain;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tgsql.core.executor.engine.EngineConfigurationException;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.executor.report.ScriptReporter;
import com.tsurugidb.tgsql.core.model.ErroneousStatement;
import com.tsurugidb.tgsql.core.model.Regioned;
import com.tsurugidb.tgsql.core.model.Value;
import com.tsurugidb.tsubakuro.explain.PlanGraph;
import com.tsurugidb.tsubakuro.explain.PlanGraphException;
import com.tsurugidb.tsubakuro.explain.PlanGraphLoader;
import com.tsurugidb.tsubakuro.explain.json.JsonPlanGraphLoader;
import com.tsurugidb.tsubakuro.sql.StatementMetadata;

/**
 * Handles {@link StatementMetadata}.
 */
public class StatementMetadataHandler implements OptionHandler {

    /**
     * The explain option name prefix of this handler.
     */
    public static final String KEY_PREFIX = "plan.";

    /**
     * The explain option name of enabling verbose execution plan.
     */
    public static final String KEY_VERBOSE = KEY_PREFIX + "verbose"; //$NON-NLS-1$

    /**
     * The explain option name of output target path of raw explain outputs.
     */
    public static final String KEY_OUTPUT = KEY_PREFIX + "output"; //$NON-NLS-1$

    private final PlanGraphLoader loader;

    private final @Nullable Path dumpOutput;

    /**
     * Creates a new instance from {@code EXPLAIN} statement options.
     * @param options the explain statement options
     * @return the created instance
     * @throws EngineConfigurationException if options are invalid
     */
    public static StatementMetadataHandler fromOptions(
            @Nonnull Map<Regioned<String>, Optional<Regioned<Value>>> options) throws EngineConfigurationException {
        Objects.requireNonNull(options);
        var loader = JsonPlanGraphLoader.newBuilder();
        if (Util.findBoolean(options, KEY_VERBOSE).orElse(false)) {
            loader.withNodeFilter(node -> true);
        }
        var dumpOutput = Util.findPath(options, KEY_OUTPUT).orElse(null);
        if (dumpOutput != null && Files.isDirectory(dumpOutput)) {
            throw new EngineConfigurationException(
                    ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION,
                    Util.findValue(options, KEY_OUTPUT).map(Regioned::getRegion).get(),
                    MessageFormat.format(
                            "output is already exists as a directory: {0}",
                            dumpOutput));
        }
        return new StatementMetadataHandler(loader.build(), dumpOutput);
    }

    /**
     * Creates a new instance.
     * @param loader the plan graph loader
     * @param dumpOutput the raw execution plan output target
     */
    public StatementMetadataHandler(@Nonnull PlanGraphLoader loader, @Nullable Path dumpOutput) {
        Objects.requireNonNull(loader);
        this.loader = loader;
        this.dumpOutput = dumpOutput;
    }

    /**
     * Returns the plan graph loader.
     * @return the plan graph loader
     */
    public PlanGraphLoader getLoader() {
        return loader;
    }

    @Override
    public boolean isHandled(@Nonnull String key) {
        Objects.requireNonNull(key);
        return key.equalsIgnoreCase(KEY_VERBOSE)
                || key.equalsIgnoreCase(KEY_OUTPUT);
    }

    /**
     * Converts {@link StatementMetadata} and converts it into {@link PlanGraph}.
     * @param reporter the status reporter
     * @param metadata the input metadata
     * @return the converted graph
     * @throws EngineException if error was occurred while processing the metadata
     */
    public PlanGraph handle(
            @Nonnull ScriptReporter reporter,
            @Nonnull StatementMetadata metadata) throws EngineException {
        Objects.requireNonNull(reporter);
        Objects.requireNonNull(metadata);
        if (dumpOutput != null) {
            try {
                dump(reporter, metadata);
            } catch (IOException e) {
                throw new EngineException(MessageFormat.format(
                        "failed to output explain result: {0}",
                        dumpOutput), e);
            }
        }
        PlanGraph graph;
        try {
            graph = loader.load(metadata.getFormatId(), metadata.getFormatVersion(), metadata.getContents());
        } catch (PlanGraphException e) {
            throw new EngineException(MessageFormat.format(
                    "unrecognized explain result: {0}; format-id={1}, format-version={2}",
                    e.getMessage(),
                    metadata.getFormatId(),
                    metadata.getFormatVersion()), e);
        }
        return graph;
    }

    private void dump(ScriptReporter reporter, StatementMetadata metadata) throws IOException {
        assert dumpOutput != null;
        Util.prepareParentDirectory(dumpOutput);
        Files.writeString(dumpOutput, metadata.getContents(), StandardCharsets.UTF_8);
        reporter.info(MessageFormat.format(
                "output raw explain result: {0}",
                dumpOutput));
    }
}
