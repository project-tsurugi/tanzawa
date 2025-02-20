/*
 * Copyright 2023-2025 Project Tsurugi.
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

import java.text.MessageFormat;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

/**
 * Normalizes name of tables or queries.
 */
public class NameNormalizer implements Function<String, String> {

    /**
     * Default name length limit.
     */
    public static final int DEFAULT_NAME_LIMIT  = 50;

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

    private final int nameLimit;

    private final BitSet escapeTargets;

    private final char replacement;

    private final char delimiter;

    /**
     * Creates a new instance with default settings.
     * @see #DEFAULT_NAME_LIMIT
     * @see #DEFAULT_RESTRICTED_CHARACTERS
     * @see #DEFAULT_REPLACEMENT
     * @see #DEFAULT_DELIMITER
     */
    public NameNormalizer() {
        this(DEFAULT_NAME_LIMIT, DEFAULT_RESTRICTED_CHARACTERS, DEFAULT_REPLACEMENT, DEFAULT_DELIMITER);
    }

    /**
     * Creates a new instance.
     * @param nameLimit the maximum name length.
     *      If exceeded, the name will be trimmed the trailing characters to the length.
     * @param escapeTargets the escape target characters, each must be ASCII character
     * @param replacement the replacement character, must be ASCII character
     * @param delimiter the delimiter character, must be ASCII character
     * @throws IllegalArgumentException if {@code nameLimit} is less than or equal to 0
     * @throws IllegalArgumentException if {@code escapeTargets} or {@code delimiter} contains non-ASCII character
     */
    public NameNormalizer(
            int nameLimit,
            @Nonnull CharSequence escapeTargets,
            char replacement,
            char delimiter) {
        if (nameLimit <= 0) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "name limit must be >= 0 ({0})",
                    nameLimit));
        }
        Objects.requireNonNull(escapeTargets);
        escapeTargets.codePoints().forEach(it -> checkAscii("escape target", it));
        checkAscii("replacement", replacement);
        checkAscii("delimiter", delimiter);
        this.nameLimit = nameLimit;
        this.escapeTargets = toBitSet(escapeTargets);
        this.replacement = replacement;
        this.delimiter = delimiter;
    }

    static BitSet toBitSet(CharSequence elements) {
        return elements.codePoints()
                .collect(
                        () -> new BitSet(ASCII_COUNT),
                        (r, c) -> r.set(c),
                        (a, b) -> a.or(b));
    }

    static List<String> fromBitSet(BitSet elements) {
        return elements.stream()
            .sorted()
            .mapToObj(Character::toString)
            .collect(Collectors.toList());
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
     * Normalizes the given table or query name.
     * @param name the name
     * @return the normalized name
     */
    @Override
    public String apply(@Nonnull String name) {
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
            .limit(nameLimit)
            .toArray();
        return new String(codePoints, 0, codePoints.length);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nameLimit, escapeTargets, delimiter, replacement);
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
        var other = (NameNormalizer) obj;
        return delimiter == other.delimiter
                && Objects.equals(escapeTargets, other.escapeTargets)
                && replacement == other.replacement
                && nameLimit == other.nameLimit;
    }

    @Override
    public String toString() {
        return String.format(
                "NamelNormalizer(nameLimit=%s, escapeTargets=%s, replacement=%s, delimiter=%s)",
                nameLimit,
                fromBitSet(escapeTargets),
                replacement,
                delimiter);
    }
}
