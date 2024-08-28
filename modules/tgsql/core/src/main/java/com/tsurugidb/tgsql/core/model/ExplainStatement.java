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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * A {@link Statement} that represents {@code EXPLAIN}.
 */
public class ExplainStatement implements Statement {

    private final String text;

    private final Region region;

    private final Statement body;

    private final Map<Regioned<String>, Optional<Regioned<Value>>> options;

    /**
     * Creates a new instance.
     * @param text the text of this statement
     * @param region the region of this statement in the document
     * @param body the explain target statement
     * @param options the explain options
     */
    public ExplainStatement(
            @Nonnull String text, @Nonnull Region region,
            @Nonnull Statement body, @Nonnull Map<Regioned<String>, Optional<Regioned<Value>>> options) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(region);
        Objects.requireNonNull(body);
        Objects.requireNonNull(options);
        this.text = text;
        this.region = region;
        this.body = body;
        this.options = Map.copyOf(options);
    }

    /**
     * Creates a new instance.
     * @param text the text of this statement
     * @param region the region of this statement in the document
     * @param body the region of body statement in the document, must be included in {@code region}
     * @param options the explain options
     * @throws IndexOutOfBoundsException if the {@code body} is out of statement {@code region}
     */
    public ExplainStatement(
            @Nonnull String text, @Nonnull Region region,
            @Nonnull Region body, @Nonnull Map<Regioned<String>, Optional<Regioned<Value>>> options) {
        Objects.requireNonNull(text);
        Objects.requireNonNull(region);
        Objects.requireNonNull(body);
        Objects.requireNonNull(options);
        int offset = (int) (body.getPosition() - region.getPosition());
        if (offset < 0 || offset + body.getSize() > region.getSize()) {
            throw new IndexOutOfBoundsException();
        }
        this.text = text;
        this.region = region;
        this.body = new SimpleStatement(
                Kind.GENERIC,
                text.substring(offset, offset + body.getSize()),
                body);
        this.options = Map.copyOf(options);
    }

    @Override
    public Kind getKind() {
        return Kind.EXPLAIN;
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
     * Returns the body statement.
     * @return the body statement
     */
    public Statement getBody() {
        return body;
    }

    /**
     * Returns explain options.
     * @return explain options
     */
    public Map<Regioned<String>, Optional<Regioned<Value>>> getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return String.format(
                "Statement(kind=%s, text='%s', region=%s, body=%s, options=%s)", //$NON-NLS-1$
                Kind.EXPLAIN,
                text,
                region,
                body,
                options);
    }
}
