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

enum TokenKind {
    /**
     * End-Of-File.
     */
    EOF(TokenCategory.DELIMITER),

    /**
     * text segment which the parser cannot handle.
     */
    UNHANDLED_TEXT(TokenCategory.UNKNOWN),

    /**
     * regular (unquoted) identifiers.
     */
    REGULAR_IDENTIFIER(TokenCategory.REGULAR),

    /**
     * delimited identifiers.
     */
    DELIMITED_IDENTIFIER(TokenCategory.REGULAR),

    // literals

    /**
     * numeric literals.
     */
    NUMERIC_LITERAL(TokenCategory.REGULAR),

    /**
     * true.
     */
    TRUE_LITERAL(TokenCategory.REGULAR),

    /**
     * false.
     */
    FALSE_LITERAL(TokenCategory.REGULAR),

    /**
     * null.
     */
    NULL_LITERAL(TokenCategory.REGULAR),

    /**
     * character string literals.
     */
    CHARACTER_STRING_LITERAL(TokenCategory.REGULAR),

    /**
     * binary string literals.
     */
    BINARY_STRING_LITERAL(TokenCategory.REGULAR),

    // punctuation

    /**
     * dot.
     */
    DOT(TokenCategory.PUNCTUATION),

    /**
     * comma.
     */
    COMMA(TokenCategory.PUNCTUATION),

    /**
     * semicolon.
     */
    SEMICOLON(TokenCategory.DELIMITER),

    /**
     * open paren.
     */
    LEFT_PAREN(TokenCategory.PUNCTUATION),

    /**
     * close paren.
     */
    RIGHT_PAREN(TokenCategory.PUNCTUATION),

    // operators

    /**
     * plus sign.
     */
    PLUS(TokenCategory.PUNCTUATION),

    /**
     * minus sign.
     */
    MINUS(TokenCategory.PUNCTUATION),

    /**
     * asterisk.
     */
    ASTERISK(TokenCategory.PUNCTUATION),

    /**
     * equal sign.
     */
    EQUAL(TokenCategory.PUNCTUATION),

    /**
     * bare back-slash sign.
     */
    BACK_SLASH(TokenCategory.PUNCTUATION),

    /**
     * special command name.
     */
    SPECIAL_COMMAND(TokenCategory.REGULAR),

    /**
     * special command argument.
     */
    SPECIAL_COMMAND_ARGUMENT(TokenCategory.REGULAR),

    /**
     * line break in special command.
     */
    LINE_BREAK(TokenCategory.DELIMITER),

    // comments

    /**
     * C-style comment block.
     */
    BLOCK_COMMENT(TokenCategory.COMMENT),

    /**
     * comments leading two slashes.
     */
    SLASH_COMMENT(TokenCategory.COMMENT),

    /**
     * comments leading two hyphens.
     */
    HYPHEN_COMMENT(TokenCategory.COMMENT),

    /**
     * pseudo-symbol of end of statement.
     */
    END_OF_STATEMENT(TokenCategory.PSEUDO),

    ;
    private final TokenCategory category;

    TokenKind(TokenCategory category) {
        assert category != null;
        this.category = category;
    }

    /**
     * Returns the category of this token.
     * @return the token category
     */
    public TokenCategory getCategory() {
        return category;
    }
}
