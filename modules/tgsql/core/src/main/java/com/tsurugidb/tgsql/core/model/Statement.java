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
package com.tsurugidb.tgsql.core.model;

/**
 * Represents a SQL statement.
 */
public interface Statement {

    /**
     * A kind of {@link Statement}.
     */
    enum Kind {

        /**
         * empty statement.
         * @see SimpleStatement
         */
        EMPTY,

        /**
         * generic SQL statement.
         * @see SimpleStatement
         */
        GENERIC,

        /**
         * {@code START TRANSACTION} statement.
         * @see StartTransactionStatement
         */
        START_TRANSACTION,

        /**
         * {@code COMMIT} statement.
         * @see CommitStatement
         */
        COMMIT,

        /**
         * {@code ROLLBACK} statement.
         * @see SimpleStatement
         */
        ROLLBACK,

        /**
         * {@code CALL} statement.
         * @see CallStatement
         */
        CALL,

        /**
         * {@code EXPLAIN} statement.
         * @see ExplainStatement
         */
        EXPLAIN,

        /**
         * {@code SPECIAL} statement.
         * @see SpecialStatement
         */
        SPECIAL,

        /**
         * erroneous statement.
         * @see ErroneousStatement
         */
        ERRONEOUS,
    }

    /**
     * Returns the statement kind.
     * @return the statement kind
     */
    Kind getKind();

    /**
     * Returns the text of this statement.
     * @return the text
     */
    String getText();

    /**
     * Returns the region of this statement in the document.
     * @return the region
     */
    Region getRegion();
}
