/*
 * Copyright 2023-2024 Project Tsurugi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tsurugidb.tools.tgdump.core.engine;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tools.tgdump.core.model.DumpTarget;

/**
 * Computes table dump destination directories from each table name.
 */
public class TableDumpTargetSelector implements DumpTargetSelector {

    private static final Logger LOG = LoggerFactory.getLogger(TableDumpTargetSelector.class);

    private static final Function<String, String> DEFAULT_NORMALIZER = new NameNormalizer();

    private Function<? super String, ? extends String> normalizer;

    private final char delimiter;

    /**
     * Creates a new instance with default settings.
     */
    public TableDumpTargetSelector() {
        this(DEFAULT_NORMALIZER, DEFAULT_SEQUENCE_DELIMITER);
    }

    /**
     * Creates a new instance.
     * @param normalizer the destination name normalizer
     * @param delimiter the separator character between name and sequence number, must be escaped within the normalizer
     */
    public TableDumpTargetSelector(@Nonnull Function<? super String, ? extends String> normalizer, char delimiter) {
        Objects.requireNonNull(normalizer); 
        this.normalizer = normalizer;
        this.delimiter = delimiter;
    }

    /**
     * Computes {@link DumpTarget dump targets} for each table.
     * @param destinationDirectory the base destination directory,
     *      each dump target will be placed under it.
     * @param commands the table name list
     * @return the dump targets for the tables
     */
    @Override
    public List<DumpTarget> getTargets(@Nonnull Path destinationDirectory, @Nonnull List<String> commands) {
        Objects.requireNonNull(destinationDirectory);
        Objects.requireNonNull(commands);
        LOG.trace("enter: getTargets: {}, {}", destinationDirectory, commands); //$NON-NLS-1$
        var results = commands.stream()
                .map(it -> it.strip())
                .peek(it -> {
                    if (it.isEmpty()) {
                        throw new IllegalArgumentException("table name must not be empty");
                    }
                })
                .map(tableName -> new DumpTarget(
                        tableName,
                        destinationDirectory.resolve(normalizer.apply(tableName))))
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        it -> DumpTargetSelector.resolveConflicts(it, delimiter)));
        LOG.trace("exit: getTargets: {}", results); //$NON-NLS-1$
        return results;
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalizer, delimiter);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        var other = (TableDumpTargetSelector) obj;
        return Objects.equals(normalizer, other.normalizer) && delimiter == other.delimiter;
    }

    @Override
    public String toString() {
        return String.format("TableDumpTargetSelector(normalizer=%s, delimiter=%s)", normalizer, delimiter);
    }
}
