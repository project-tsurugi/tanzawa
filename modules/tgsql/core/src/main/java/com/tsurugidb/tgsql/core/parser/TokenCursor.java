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
package com.tsurugidb.tgsql.core.parser;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.tsurugidb.tgsql.core.model.Region;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class TokenCursor {

    private final Segment segment;

    private int cursor = 0;

    TokenCursor(@Nonnull Segment segment) {
        Objects.requireNonNull(segment);
        this.segment = segment;
    }

    Optional<TokenInfo> lookahead(int offset) {
        var ts = segment.getTokens();
        if (cursor + offset >= ts.size()) {
            return Optional.empty();
        }
        return Optional.of(ts.get(cursor + offset));
    }

    TokenInfo token(int offset) {
        return checkOffset(offset);
    }

    Region region(int offset) {
        var token = checkOffset(offset);
        return regionOf(token);
    }

    private Region regionOf(TokenInfo token) {
        return new Region(
                segment.getOffset() + token.getOffset(),
                token.getLength(),
                token.getStartLine(),
                token.getStartColumn());
    }

    Region lastRegion() {
        var ts = segment.getTokens();
        if (ts.isEmpty()) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = ts.size() - 1; i >= 0; --i) {
            var t = ts.get(i);
            if (t.getKind() != TokenKind.END_OF_STATEMENT) {
                return regionOf(t);
            }
        }
        return regionOf(ts.get(0));
    }

    String text(int offset) {
        var token = checkOffset(offset);
        return segment.getText(token).get();
    }

    Region region(int first, int last) {
        return region(first).union(region(last));
    }

    @SuppressFBWarnings(
            value = "RV",
            justification = "misdetection: segment.getTokens() may raise an exception")
    public void consume(int count) {
        checkOffset(count - 1);
        cursor += count;
    }

    private TokenInfo checkOffset(int offset) {
        var ts = segment.getTokens();
        if (cursor + offset >= ts.size()) {
            throw new IndexOutOfBoundsException("cursor index out of bounds"); //$NON-NLS-1$
        }
        return ts.get(cursor + offset);
    }
}
