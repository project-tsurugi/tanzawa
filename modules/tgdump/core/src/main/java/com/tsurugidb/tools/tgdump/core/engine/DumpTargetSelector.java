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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tools.tgdump.core.model.DumpTarget;

/**
 * Computes table dump destination directories from each table name.
 */
public class DumpTargetSelector {

    /**
     * Default table name length limit.
     */
    public static final int DEFAULT_TABLE_NAME_LIMIT  = 50;

    /**
     * Default restricted characters.
     */
    public static final String DEFAULT_RESTRICTED_CHARACTERS = ".<>:/\\|?*\"";

    /**
     * Default replacement character.
     */
    public static final char DEFAULT_REPLACEMENT = '_';

    /**
     * Default delimiter character.
     */
    public static final char DEFAULT_DELIMITER = '-';

    private static final int ASCII_COUNT = 128;

    private static final Logger LOG = LoggerFactory.getLogger(DumpTargetSelector.class);

    private final int tableNameLimit;

    private final BitSet escapeTargets;

    private final char replacement;

    private final char delimiter;

    /**
     * Creates a new instance with default settings.
     * @see #DEFAULT_TABLE_NAME_LIMIT
     * @see #DEFAULT_RESTRICTED_CHARACTERS
     * @see #DEFAULT_REPLACEMENT
     * @see #DEFAULT_DELIMITER
     */
    public DumpTargetSelector() {
        this(DEFAULT_TABLE_NAME_LIMIT, DEFAULT_RESTRICTED_CHARACTERS, DEFAULT_REPLACEMENT, DEFAULT_DELIMITER);
    }

    /**
     * Creates a new instance.
     * @param tableNameLimit the maximum table name length.
     *      If exceeded, the table name will be trimmed the trailing characters to the length.
     * @param escapeTargets the escape target characters, each must be ASCII character
     * @param replacement the replacement character, must be ASCII character
     * @param delimiter the delimiter character, must be ASCII character
     * @throws IllegalArgumentException if {@code tableNameLimit} is less than or equal to 0
     * @throws IllegalArgumentException if {@code escapeTargets} or {@code delimiter} contains non-ASCII character
     */
    public DumpTargetSelector(
            int tableNameLimit,
            @Nonnull CharSequence escapeTargets,
            char replacement,
            char delimiter) {
        if (tableNameLimit <= 0) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "table name limit must be >= 0 ({0})",
                    tableNameLimit));
        }
        Objects.requireNonNull(escapeTargets);
        escapeTargets.codePoints().forEach(it -> checkAscii("escape target", it));
        checkAscii("replacement", replacement);
        checkAscii("delimiter", delimiter);
        this.tableNameLimit = tableNameLimit;
        this.escapeTargets = escapeTargets.codePoints()
                .collect(
                        () -> new BitSet(ASCII_COUNT),
                        (r, c) -> r.set(c),
                        (a, b) -> a.or(b));
        this.replacement = replacement;
        this.delimiter = delimiter;
    }

    private static void checkAscii(String name, int value) {
        if (value >= ASCII_COUNT) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "{0} must be an ASCII character: {1}",
                    name,
                    String.format("U+%04X", value))); //$NON-NLS-1$
        }
    }

    /**
     * Normalizes the given table name.
     * @param name the table name
     * @return the normalized name
     */
    public String normalize(@Nonnull String name) {
        Objects.requireNonNull(name);
        var codePoints = name.codePoints()
            .sequential()
            .map(c -> {
                if (Character.isISOControl(c)
                        || Character.isWhitespace(c)
                        || Character.isSupplementaryCodePoint(c)
                        || c == delimiter
                        || escapeTargets.get(c)) {
                    return replacement;
                } else if (Character.isUpperCase(c)) {
                    return Character.toLowerCase(c);
                }
                return c;
            })
            .limit(tableNameLimit)
            .toArray();
        return new String(codePoints, 0, codePoints.length);
    }

    /**
     * Computes {@link DumpTarget dump targets} for each table.
     * @param destinationDirectory the base destination directory,
     *      each dump target will be placed under it.
     * @param tableNames the table name list
     * @return the dump targets for the tables
     */
    public List<DumpTarget> getTargets(@Nonnull Path destinationDirectory, @Nonnull List<String> tableNames) {
        Objects.requireNonNull(destinationDirectory);
        Objects.requireNonNull(tableNames);
        LOG.trace("enter: getTargets: {}, {}", destinationDirectory, tableNames); //$NON-NLS-1$

        var conflictCounts = new HashMap<String, AtomicInteger>();
        var results = new ArrayList<DumpTarget>(tableNames.size());

        for (var name : tableNames) {
            var normalized = normalize(name);
            int conflicts = conflictCounts.computeIfAbsent(normalized, k -> new AtomicInteger()).getAndIncrement();
            Path destination;
            if (conflicts <= 0) {
                destination = destinationDirectory.resolve(normalized);
            } else {
                destination = destinationDirectory.resolve(addSuffix(normalized, conflicts));
            }
            var target = new DumpTarget(name, destination);
            LOG.trace("element: getTargets: {}", target); //$NON-NLS-1$
            results.add(target);
        }

        LOG.trace("exit: getTargets: {}", results); //$NON-NLS-1$
        return results;
    }

    private String addSuffix(String base, int index) {
        return String.format("%s%s%d", base, delimiter, index); //$NON-NLS-1$
    }

    @Override
    public int hashCode() {
        return Objects.hash(tableNameLimit, escapeTargets, delimiter, replacement);
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
        DumpTargetSelector other = (DumpTargetSelector) obj;
        return delimiter == other.delimiter
                && Objects.equals(escapeTargets, other.escapeTargets)
                && replacement == other.replacement
                && tableNameLimit == other.tableNameLimit;
    }

    @Override
    public String toString() {
        return String.format(
                "DumpTargetPlacer(tableNameLimit=%s, escapeTargets=%s, replacement=%s, delimiter=%s)",
                tableNameLimit,
                escapeTargets.stream()
                    .mapToObj(Character::toString)
                    .collect(Collectors.joining(", ", "{", "}")), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                replacement,
                delimiter);
    }
}
