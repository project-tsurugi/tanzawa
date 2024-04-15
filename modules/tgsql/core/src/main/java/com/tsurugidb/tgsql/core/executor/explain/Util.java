package com.tsurugidb.tgsql.core.executor.explain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.executor.engine.EngineConfigurationException;
import com.tsurugidb.tgsql.core.model.ErroneousStatement;
import com.tsurugidb.tgsql.core.model.Regioned;
import com.tsurugidb.tgsql.core.model.Value;

final class Util {

    static final Logger LOG = LoggerFactory.getLogger(Util.class);

    static Optional<Path> findPath(
            @Nonnull Map<Regioned<String>, Optional<Regioned<Value>>> options,
            @Nonnull String key) throws EngineConfigurationException {
        Objects.requireNonNull(options);
        Objects.requireNonNull(key);
        var found = findEntry(options, key, Value.of());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        var path = found.get().getValue().getValue().asCharacter()
                .orElseThrow(() -> new EngineConfigurationException(
                        ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION,
                        found.get().getValue().getRegion(),
                        MessageFormat.format(
                                "explain option \"{0}\" must be a valid file path",
                                found.get().getKey().getValue())));
        return Optional.of(Path.of(path));
    }

    static Optional<Boolean> findBoolean(
            @Nonnull Map<Regioned<String>, Optional<Regioned<Value>>> options,
            @Nonnull String key) throws EngineConfigurationException {
        Objects.requireNonNull(options);
        Objects.requireNonNull(key);
        var found = findEntry(options, key, Value.of(true));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        var value = found.get().getValue().getValue().asBoolean()
                .orElseThrow(() -> new EngineConfigurationException(
                        ErroneousStatement.ErrorKind.INVALID_EXPLAIN_OPTION,
                        found.get().getValue().getRegion(),
                        MessageFormat.format(
                                "explain option \"{0}\" must be a boolean value",
                                found.get().getKey().getValue())));
        return Optional.of(value);
    }

    static Optional<Regioned<String>> findKey(
            @Nonnull Map<Regioned<String>, Optional<Regioned<Value>>> options,
            @Nonnull String key) {
        Objects.requireNonNull(options);
        Objects.requireNonNull(key);
        return findEntry(options, key, Value.of())
                .map(Map.Entry::getKey);
    }

    static Optional<Regioned<Value>> findValue(
            @Nonnull Map<Regioned<String>, Optional<Regioned<Value>>> options,
            @Nonnull String key) {
        Objects.requireNonNull(options);
        Objects.requireNonNull(key);
        return findEntry(options, key, Value.of())
                .map(Map.Entry::getValue);
    }

    static Optional<Map.Entry<Regioned<String>, Regioned<Value>>> findEntry(
            @Nonnull Map<Regioned<String>, Optional<Regioned<Value>>> options,
            @Nonnull String key,
            @Nonnull Value keyOnlyValue) {
        Objects.requireNonNull(options);
        Objects.requireNonNull(key);
        Objects.requireNonNull(keyOnlyValue);
        return options.entrySet().stream()
                .filter(it -> it.getKey().getValue().equalsIgnoreCase(key))
                .map(it -> Map.entry(
                        it.getKey(),
                        it.getValue().orElseGet(() -> it.getKey().getRegion().wrap(keyOnlyValue))))
                .findFirst();
    }

    static void prepareParentDirectory(@Nonnull Path path) throws IOException {
        Objects.requireNonNull(path);
        LOG.trace("creating parent directory: {}", path); //$NON-NLS-1$
        var parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private Util() {
        throw new AssertionError();
    }
}
