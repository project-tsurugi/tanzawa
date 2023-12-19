package com.tsurugidb.tools.common.value;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a key-value pair.
 */
public class Property {

    private final String name;

    private final Value value;

    /**
     * Creates a new instance.
     * @param name the property name
     * @param value the property value
     */
    public Property(@Nonnull String name, @Nonnull Value value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);
        this.name = name;
        this.value = value;
    }

    /**
     * Creates a new instance.
     * @param name the property name
     * @param value the property value as object
     * @return the created instance
     * @throws IllegalArgumentException if it contains a value that not suitable for the property value
     */
    public static Property of(@Nonnull String name, @Nullable Object value) {
        Objects.requireNonNull(name);
        return new Property(name, Value.fromObject(value));
    }

    /**
     * Returns the property name.
     * @return the property name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the property value.
     * @return the property value
     */
    public Value getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
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
        Property other = (Property) obj;
        return Objects.equals(name, other.name) && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        return String.format("%s:%s", name, value); //$NON-NLS-1$
    }
}
