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
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A {@link Statement} that represents {@code COMMIT}.
 */
public class CommitStatement implements Statement {

    /**
     * The commit status.
     */
    public enum CommitStatus {

        /**
         * Commit is accepted and it will never lost except system errors.
         */
        ACCEPTED,

        /**
         * Commit is available for other transactions and it will never lost except system errors.
         */
        AVAILABLE,

        /**
         * Commit is stored to the local file system.
         */
        STORED,

        /**
         * Commit is propagated to the whole system.
         */
        PROPAGATED,
    }

    private final String text;

    private final Region region;

    private final Regioned<CommitStatus> commitStatus;

    /**
     * Creates a new instance.
     * @param text the text of this statement
     * @param region the region of this statement in the document
     * @param commitStatus the commit status which the client will wait for
     */
    public CommitStatement(
            @Nonnull String text,
            @Nonnull Region region,
            @Nullable Regioned<CommitStatus> commitStatus) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(region);
        this.text = text;
        this.region = region;
        this.commitStatus = commitStatus;
    }

    @Override
    public Kind getKind() {
        return Kind.COMMIT;
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
     * Returns the commit status which the client will wait for.
     * @return the commit status
     */
    public Optional<Regioned<CommitStatus>> getCommitStatus() {
        return Optional.ofNullable(commitStatus);
    }

    @Override
    public String toString() {
        return String.format(
                "Statement(kind=%s, text='%s', region=%s, commitStatus=%s)", //$NON-NLS-1$
                getKind(),
                text,
                region,
                commitStatus);
    }
}
