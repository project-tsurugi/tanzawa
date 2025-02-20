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

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tools.tgdump.core.model.DumpTarget;

/**
 * Computes dump destination directories from each SQL text.
 */
public class QueryDumpTargetSelector implements DumpTargetSelector {

    private static final Logger LOG = LoggerFactory.getLogger(QueryDumpTargetSelector.class);

    private static final Function<String, String> DEFAULT_NORMALIZER = new NameNormalizer();

    /**
     * Default restricted characters.
     */
    public static final String DEFAULT_STOP_CHARACTERS = "\"'";

    /**
     * Default value of the default label prefix.
     */
    public static final String DEFAULT_DEFAULT_PREFIX = "sql";

    /**
     * Default delimiter character between label and statement.
     */
    public static final char DEFAULT_LABEL_DELIMITER = ':';

    private static final String EMPTY_TEXT = ""; //$NON-NLS-1$

    enum State {
        INITIAL,
        BODY,
        PADDING,
        FINISH,
    }

    enum CharType {
        NORMAL,
        SPACE,
        STOP,
        DELIMITER,
    }

    static class LabelAndStatement {

        private final @Nullable String label;

        private final String statement;

        LabelAndStatement(@Nonnull String statement) {
            this(null, statement);
        }

        LabelAndStatement(String label, @Nonnull String statement) {
            Objects.requireNonNull(statement);
            this.label = label == null ? null : label.strip();
            this.statement = statement.strip();
        }

        Optional<String> getLabel() {
            return Optional.ofNullable(label);
        }

        String getStatement() {
            return statement;
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, statement);
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
            var other = (LabelAndStatement) obj;
            return Objects.equals(label, other.label) && Objects.equals(statement, other.statement);
        }

        @Override
        public String toString() {
            return String.format("(%s, %s)", label, statement); //$NON-NLS-1$
        }
    }

    private Function<? super String, ? extends String> normalizer;

    private final char labelDelimiter;

    private final String defaultPrefix;

    private final BitSet stopCharacters;

    private final char sequenceDelimiter;

    /**
     * Creates a new instance with default settings.
     */
    public QueryDumpTargetSelector() {
        this(
                DEFAULT_NORMALIZER,
                DEFAULT_LABEL_DELIMITER,
                DEFAULT_STOP_CHARACTERS,
                DEFAULT_DEFAULT_PREFIX,
                DEFAULT_SEQUENCE_DELIMITER);
    }

    /**
     * Creates a new instance.
     * @param normalizer the destination name normalizer
     * @param delimiter the delimiter character between label and statement
     * @param stopCharacters the characters which stops the label
     * @param defaultPrefix the prefix if label is not specified
     * @param sequenceDelimiter the delimiter character for path and its sequence
     * @throws IllegalArgumentException if {@code stopCharacters} contains non-ASCII character
     */
    public QueryDumpTargetSelector(
            @Nonnull Function<? super String, ? extends String> normalizer, 
            char delimiter,
            @Nonnull CharSequence stopCharacters,
            @Nonnull String defaultPrefix,
            char sequenceDelimiter) {
        Objects.requireNonNull(normalizer);
        Objects.requireNonNull(stopCharacters);
        Objects.requireNonNull(defaultPrefix);
        this.normalizer = normalizer;
        this.labelDelimiter = delimiter;
        this.defaultPrefix = defaultPrefix;
        this.stopCharacters = NameNormalizer.toBitSet(stopCharacters);
        this.sequenceDelimiter = sequenceDelimiter;
    }

    LabelAndStatement parseCommand(String command) {
        LOG.trace("enter: parseCommand: {}", command); //$NON-NLS-1$
        var result = parseCommand0(command);
        LOG.trace("exit: parseCommand: {}", result); //$NON-NLS-1$
        return result;
    }

    private LabelAndStatement parseCommand0(String command) {
        var state = State.INITIAL;
        int labelStart = -1;
        int labelEnd = -1;
        for (int position = 0, n = command.length(); position < n; position++) {
            var c = command.charAt(position);
            var type = classifyChar(c);
            if (LOG.isTraceEnabled()) {
                LOG.trace("step: parseCommand: position={}, state={}, type={}", position, state, type); //$NON-NLS-1$
            }
            switch (state) {
                case INITIAL:
                    switch (type) {
                        case NORMAL:
                            state = State.BODY;
                            labelStart = position;
                            break;
                        case SPACE:
                            // continue
                            break;
                        case DELIMITER:
                            state = State.FINISH;
                            labelStart = position;
                            labelEnd = position;
                            break;
                        default:
                            return new LabelAndStatement(command);
                    }
                    break;
                case BODY:
                    switch (type) {
                        case NORMAL:
                            // continue
                            break;
                        case SPACE:
                            state = State.PADDING;
                            labelEnd = position;
                            break;
                        case DELIMITER:
                            state = State.FINISH;
                            labelEnd = position;
                            break;
                        default:
                            return new LabelAndStatement(command);
                    }
                    break;
                case PADDING:
                    switch (type) {
                        case NORMAL:
                            return new LabelAndStatement(command);
                        case SPACE:
                            // continue
                            break;
                        case DELIMITER:
                            state = State.FINISH;
                            break;
                        default:
                            return new LabelAndStatement(command);
                    }
                    break;
                case FINISH:
                    switch (type) {
                        case SPACE:
                            // continue
                            break;
                        default:
                            return new LabelAndStatement(
                                command.substring(labelStart, labelEnd),
                                command.substring(position));
                    }
                    break;
                default:
                    throw new AssertionError(); // never reach
            }
        }
        if (state == State.FINISH) {
            // statement is empty
            return new LabelAndStatement(
                command.substring(labelStart, labelEnd),
                EMPTY_TEXT);
        }
        // delimiter not found
        return new LabelAndStatement(command);
    }

    private CharType classifyChar(char c) {
        if (Character.isWhitespace(c)) {
            return CharType.SPACE;
        } else if (Character.isISOControl(c) || stopCharacters.get(c)) {
            return CharType.STOP;
        } else if (c == labelDelimiter) {
            return CharType.DELIMITER;
        }
        return CharType.NORMAL;
    }

    @Override
    public DumpTarget getTarget(@Nonnull Path destinationDirectory, @Nonnull String command) {
        Objects.requireNonNull(destinationDirectory);
        Objects.requireNonNull(command);
        LOG.trace("enter: getTarget: {}, {}", destinationDirectory, command); //$NON-NLS-1$
        var parsed = parseCommand(command);
        var label = parsed.getLabel().orElse(defaultPrefix);
        var statement = parsed.getStatement();
        if (label.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "label must not be empty: {0}",
                    command));
        }
        if (statement.isEmpty()) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "statement must not be empty: {0}",
                    command));
        }
        var result = new DumpTarget(DumpTarget.TargetType.QUERY, label, statement, destinationDirectory);
        LOG.trace("exit: getTarget: {}", result); //$NON-NLS-1$
        return result;
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
        var targets = new ArrayList<DumpTarget>();
        for (var command : commands) {
            int position = targets.size() + 1;
            var parsed = parseCommand(command);
            var label = parsed.getLabel()
                    .orElseGet(() -> {
                        return String.format("%s%d", defaultPrefix, position); //$NON-NLS-1$
                    }); //$NON-NLS-1$
            var statement = parsed.getStatement().strip();
            if (label.isEmpty()) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "label at {0} must not be empty: {1}",
                        position,
                        command));
            }
            if (statement.isEmpty()) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "statement at {0} must not be empty: {1}",
                        position,
                        command));
            }
            var destination = destinationDirectory.resolve(normalizer.apply(label));
            targets.add(new DumpTarget(DumpTarget.TargetType.QUERY, label, statement, destination));
        }
        var results = DumpTargetSelector.resolveConflicts(targets, sequenceDelimiter);
        LOG.trace("exit: getTargets: {}", results); //$NON-NLS-1$
        return results;
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalizer, labelDelimiter, defaultPrefix, stopCharacters);
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
        var other = (QueryDumpTargetSelector) obj;
        return Objects.equals(normalizer, other.normalizer) && labelDelimiter == other.labelDelimiter
                && Objects.equals(defaultPrefix, other.defaultPrefix)
                && Objects.equals(stopCharacters, other.stopCharacters);
    }

    @Override
    public String toString() {
        return String.format(
                "QueryDumpTargetSelector(normalizer=%s, delimiter=%s, defaultPrefix=%s, stopCharacters=%s)",
                normalizer,
                labelDelimiter,
                defaultPrefix,
                NameNormalizer.fromBitSet(stopCharacters));
    }
}
