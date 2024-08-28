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
package com.tsurugidb.tools.common.value;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a value for {@link Property properties}.
 */
public final class Value {

    /**
     * Represents a kind of {@link Value}.
     */
    public enum Kind {

        /**
         * The null value.
         */
        NULL,

        /**
         * Boolean values.
         */
        BOOLEAN,

        /**
         * Integers.
         */
        INTEGER,

        /**
         * Decimal numbers.
         */
        DECIMAL,

        /**
         * Character strings.
         */
        STRING,

        /**
         * Array of values.
         */
        ARRAY,

        /**
         * Records (list of properties).
         * @see Property
         */
        RECORD,
        ;
    }

    /**
     * Represents a {@code null} value.
     */
    public static final Value NULL_VALUE = new Value(Kind.NULL, null);

    private final Kind kind;

    private final Object entity;

    private Value(Kind kind, Object entity) {
        this.kind = kind;
        this.entity = entity;
    }

    /**
     * Converts an object into the corresponding {@link Value}.
     * @param object the source value
     * @return the corresponding instance
     * @throws IllegalArgumentException if the {@code object} is not suitable for {@link Value}
     */
    public static Value fromObject(@Nullable Object object) {
        if (object == null) {
            return NULL_VALUE;
        }
        if (object instanceof Value) {
            return (Value) object;
        }
        if (object instanceof Boolean) {
            return of((Boolean) object);
        }
        if (object instanceof Integer) {
            return of((int) object);
        }
        if (object instanceof Long) {
            return of((Long) object);
        }
        if (object instanceof BigDecimal) {
            return of((BigDecimal) object);
        }
        if (object instanceof String) {
            return of((String) object);
        }
        if (object instanceof Array) {
            return of((Array) object);
        }
        if (object instanceof Record) {
            return of((Record) object);
        }
        throw new IllegalArgumentException(MessageFormat.format(
                "unsupported value: type={0}, value={1}", //$NON-NLS-1$
                object.getClass().getName(),
                String.valueOf(object)));
    }

    private void checkKind(Kind expected) {
        if (kind != expected) {
            throw new IllegalStateException(MessageFormat.format(
                    "inconsistent value kind: expected={0}, actual={1}", //$NON-NLS-1$
                    expected,
                    kind));
        }
    }

    /**
     * Returns a {@code null} value.
     * @return the created property object
     */
    public static Value ofNull() {
        return NULL_VALUE;
    }

    /**
     * Returns whether or not the value represents just {@code null}.
     * @return {@code true} if the value represents {@code null}, otherwise {@code false}
     */
    public boolean isNull() {
        return kind == Kind.NULL;
    }

    /**
     * Returns a boolean value.
     * @param value the property value
     * @return the created property object
     */
    public static Value of(boolean value) {
        return new Value(Kind.BOOLEAN, value);
    }

    /**
     * Returns as a boolean value.
     * @return the corresponding boolean value
     * @throws IllegalStateException if value kind is mismatched
     * @see #getKind()
     * @see Value.Kind#BOOLEAN
     */
    public boolean asBoolean() {
        checkKind(Kind.BOOLEAN);
        return (boolean) entity;
    }

    /**
     * Returns a integer.
     * @param value the property value
     * @return the created property object
     */
    public static Value of(int value) {
        return new Value(Kind.INTEGER, (long) value);
    }

    /**
     * Returns a integer.
     * @param value the property value
     * @return the created property object
     */
    public static Value of(long value) {
        return new Value(Kind.INTEGER, value);
    }

    /**
     * Returns as an integer.
     * @return the corresponding integer
     * @throws IllegalStateException if value kind is mismatched
     * @see #getKind()
     * @see Value.Kind#INTEGER
     */
    public long asInteger() {
        checkKind(Kind.INTEGER);
        return (long) entity;
    }

    /**
     * Returns a decimal value.
     * @param value the property value
     * @return the created property object
     */
    public static Value of(@Nonnull BigDecimal value) {
        Objects.requireNonNull(value);
        return new Value(Kind.DECIMAL, value);
    }

    /**
     * Returns as a decimal value.
     * @return the corresponding decimal value
     * @throws IllegalStateException if value kind is mismatched
     * @see #getKind()
     * @see Value.Kind#DECIMAL
     */
    public BigDecimal asDecimal() {
        checkKind(Kind.DECIMAL);
        return (BigDecimal) entity;
    }

    /**
     * Returns a character string value.
     * @param value the property value
     * @return the created property object
     */
    public static Value of(@Nonnull String value) {
        Objects.requireNonNull(value);
        return new Value(Kind.STRING, value);
    }

    /**
     * Returns as a character string value.
     * @return the corresponding character string value
     * @throws IllegalStateException if value kind is mismatched
     * @see #getKind()
     * @see Value.Kind#STRING
     */
    public String asString() {
        checkKind(Kind.STRING);
        return (String) entity;
    }

    /**
     * Returns an array value.
     * @param value the property value
     * @return the created property object
     */
    public static Value of(@Nonnull Array value) {
        Objects.requireNonNull(value);
        return new Value(Kind.ARRAY, value);
    }

    /**
     * Returns as an array.
     * @return the corresponding array
     * @throws IllegalStateException if value kind is mismatched
     * @see #getKind()
     * @see Value.Kind#ARRAY
     */
    public Array asArray() {
        checkKind(Kind.ARRAY);
        return (Array) entity;
    }

    /**
     * Returns an list record.
     * @param value the property value
     * @return the created property object
     */
    public static Value of(@Nonnull Record value) {
        Objects.requireNonNull(value);
        return new Value(Kind.RECORD, value);
    }

    /**
     * Returns as a record.
     * @return the corresponding record
     * @throws IllegalStateException if value kind is mismatched
     * @see #getKind()
     * @see Value.Kind#RECORD
     */
    public Record asRecord() {
        checkKind(Kind.RECORD);
        return (Record) entity;
    }

    /**
     * Returns the value kind.
     * @return the value kind
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * Returns the value entity.
     * @return the value entity, or {@code null} if the value just represents {@code null}
     */
    public Object getEntity() {
        return entity;
    }

    /**
     * Returns whether or not this value has the specified contents.
     *
     * <p>
     * This is equivalent to {@code Objects.equals(contents, value.getEntity())}.
     * </p>
     * @param contents the contents to test
     * @return {@code true} if this value has the contents, otherwise {@code false}
     */
    public boolean has(@Nonnull Object contents) {
        return Objects.equals(contents, entity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, entity);
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
        Value other = (Value) obj;
        return kind == other.kind && Objects.equals(entity, other.entity);
    }

    @Override
    public String toString() {
        return String.valueOf(entity);
    }
}
