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
 * A {@link Statement} which does not have any extra information.
 */
public class SimpleStatement implements Statement {

    private final Kind kind;

    private final String text;

    private final Region region;

    /**
     * Creates a new instance.
     * @param kind the statement kind
     * @param text the text of this statement
     * @param region the region of this statement in the document
     */
    public SimpleStatement(@Nonnull Kind kind, @Nonnull String text, @Nonnull Region region) {
        Objects.requireNonNull(kind);
        Objects.requireNonNull(text);
        Objects.requireNonNull(region);
        this.kind = kind;
        this.text = text;
        this.region = region;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    @Override
    public String toString() {
        return String.format(
                "Statement(kind=%s, text='%s', region=%s)", //$NON-NLS-1$
                kind,
                text,
                region);
    }
}
