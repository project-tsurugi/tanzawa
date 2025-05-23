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
package com.tsurugidb.tgsql.core.model;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * A {@link Statement} that contains syntax errors.
 */
public class ErroneousStatement implements Statement {

    /**
     * Represents an erroneous kind.
     */
    public enum ErrorKind {
        /**
         * Unexpected token was appeared.
         */
        UNEXPECTED_TOKEN,

        /**
         * conflict property key names.
         */
        CONFLICT_PROPERTIES_KEY,

        /**
         * conflict read-write mode option in {@code START TRANSACTION}.
         * @see StartTransactionStatement#getReadWriteMode()
         */
        CONFLICT_READ_WRITE_MODE_OPTION,

        /**
         * conflict exclusive mode option in {@code START TRANSACTION}.
         * @see StartTransactionStatement#getExclusiveMode()
         */
        CONFLICT_EXCLUSIVE_MODE_OPTION,

        /**
         * conflict write preserve option in {@code START TRANSACTION}.
         * @see StartTransactionStatement#getWritePreserve()
         */
        DUPLICATE_WRITE_PRESERVE_OPTION,

        /**
         * conflict include ddl option in {@code START TRANSACTION}.
         * @see StartTransactionStatement#getIncludeDdl()
         */
        DUPLICATE_INCLUDE_DDL_OPTION,

        /**
         * conflict read area option in {@code START TRANSACTION}.
         * @see StartTransactionStatement#getReadAreaInclude()
         * @see StartTransactionStatement#getReadAreaExclude()
         */
        DUPLICATE_READ_AREA_OPTION,

        /**
         * conflict transaction label option in {@code START TRANSACTION}.
         * @see StartTransactionStatement#getLabel()
         */
        DUPLICATE_TRANSACTION_LABEL_OPTION,

        /**
         * conflict transaction properties option in {@code START TRANSACTION}.
         * @see StartTransactionStatement#getProperties()
         */
        DUPLICATE_TRANSACTION_PROPERTIES_OPTION,

        /**
         * invalid commit status in {@code COMMIT}.
         * @see CommitStatement#getCommitStatus()
         */
        UNKNOWN_COMMIT_STATUS,

        /**
         * explain statement does not contain an explain target body.
         * @see ExplainStatement#getBody()
         */
        MISSING_EXPLAIN_BODY,

        /**
         * unknown explain statement option was specified.
         * @see ExplainStatement#getOptions()
         */
        UNKNOWN_EXPLAIN_OPTION,

        /**
         * invalid explain statement option was specified.
         * @see ExplainStatement#getOptions()
         */
        INVALID_EXPLAIN_OPTION,

        /**
         * invalid special command.
         * @see SpecialStatement#getCommandName()
         */
        UNKNOWN_SPECIAL_COMMAND,

        /**
         * invalid special command option.
         * @see SpecialStatement#getCommandOptions()
         */
        UNKNOWN_SPECIAL_COMMAND_OPTION,

        /**
         * Unexpected grammar.
         */
        UNSUPPORTED_GRAMMAR,
    }

    private final String text;

    private final Region region;

    private final ErrorKind errorKind;

    private final Region occurrence;

    private final String message;

    /**
     * Creates a new instance.
     * @param text the text of this statement
     * @param region the region of this statement in the document
     * @param errorKind the error kind
     * @param occurrence the error occurrence region
     * @param message the error message
     */
    public ErroneousStatement(
            @Nonnull String text,
            @Nonnull Region region,
            @Nonnull ErrorKind errorKind,
            @Nonnull Region occurrence,
            @Nonnull String message) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(region);
        Objects.requireNonNull(errorKind);
        Objects.requireNonNull(occurrence);
        Objects.requireNonNull(message);
        this.text = text;
        this.region = region;
        this.errorKind = errorKind;
        this.occurrence = occurrence;
        this.message = message;
    }

    @Override
    public Kind getKind() {
        return Kind.ERRONEOUS;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    /**
     * Returns the error kind.
     * @return the error kind
     */
    public ErrorKind getErrorKind() {
        return errorKind;
    }

    /**
     * Returns the error occurrence region.
     * @return the error occurrence region
     */
    public Region getOccurrence() {
        return occurrence;
    }

    /**
     * Returns the error message.
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format(
                "Statement(kind=%s, text='%s', region=%s, errorKind=%s, occurrence=%s, message='%s')", //$NON-NLS-1$
                getKind(),
                text,
                region,
                errorKind,
                occurrence,
                message);
    }
}
