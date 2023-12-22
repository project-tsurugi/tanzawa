package com.tsurugidb.tools.tgdump.core.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.sql.proto.SqlRequest;

/**
 * Detail settings of dump operations, including dump file format.
 */
public class DumpProfile {

    /**
     * A builder of {@link DumpProfile}.
     */
    public static class Builder {

        @Nullable String title;

        @Nullable String description;

        final Map<String, String> localizedDescriptions = new HashMap<>();

        @Nullable DumpFileFormat fileFormat;

        /**
         * Sets the profile title.
         * @param value the title string, or {@code null} if unset it.
         * @return this
         */
        public Builder withTitle(@Nullable String value) {
            this.title = value;
            return this;
        }

        /**
         * Sets the profile description.
         * @param value the description, or {@code null} if unset it.
         * @return this
         */
        public Builder withDescription(@Nullable String value) {
            this.description = value;
            return this;
        }

        /**
         * Sets the localized profile description.
         * @param language the target language
         * @param value the description, or {@code null} if unset it.
         * @return this
         */
        public Builder withLocalizedDescription(@Nonnull String language, @Nullable String value) {
            Objects.requireNonNull(language);
            var lang = normalizeLanguage(language);
            if (value == null) {
                this.localizedDescriptions.remove(lang);
            } else {
                this.localizedDescriptions.put(lang, value);
            }
            return this;
        }

        /**
         * Sets the dump file format description.
         * @param value the description, or {@code null} if unset it.
         * @return this
         */
        public Builder withFileFormat(@Nullable DumpFileFormat value) {
            this.fileFormat = value;
            return this;
        }

        /**
         * Creates a new instance from this builder settings.
         * @return the created instance
         */
        public DumpProfile build() {
            return new DumpProfile(this);
        }
    }

    private final @Nullable String title;

    private final @Nullable String description;

    private final Map<String, String> localizedDescriptions;

    private final DumpFileFormat fileFormat;

    /**
     * Creates a new instance with default settings.
     * @see #newBuilder()
     */
    public DumpProfile() {
        this(new Builder());
    }

    /**
     * Creates a new instance from the builder.
     * @param builder the source builder
     * @see #newBuilder()
     */
    public DumpProfile(@Nonnull Builder builder) {
        Objects.requireNonNull(builder);
        this.title = builder.title;
        this.description = builder.description;
        this.localizedDescriptions = new TreeMap<>(builder.localizedDescriptions);
        this.fileFormat = builder.fileFormat;
    }

    /**
     * Creates a new builder object for this class.
     * @return the created builder object
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Returns the title of this profile.
     * @return the profile title
     */
    public Optional<String> getTitle() {
        return Optional.ofNullable(title);
    }

    /**
     * Returns the description of this profile.
     * @return the profile description, or {@code empty} if it is not defined.
     */
    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the localized description of this profile.
     * <p>
     * This may returns {@code empty} even if {@link #getDescription() non-localized description} exists.
     * </p>
     * @param language the language code for the description
     * @return the localized profile description for the language, or {@code empty} if it is not sure
     */
    public Optional<String> getLocalizedDescription(@Nonnull String language) {
        Objects.requireNonNull(language);
        // normalize language
        var lang = normalizeLanguage(language);
        return Optional.ofNullable(localizedDescriptions.get(lang));
    }

    /**
     * Returns the localized description of this profile.
     * <p>
     * This may returns {@code empty} even if {@link #getDescription() non-localized description} exists.
     * </p>
     * @param locale the locale for the description
     * @return the localized profile description, or {@code empty} if it is not sure
     */
    public Optional<String> getLocalizedDescription(@Nonnull Locale locale) {
        Objects.requireNonNull(locale);
        return Optional.of(locale.getLanguage())
                .filter(it -> !it.isEmpty())
                .flatMap(this::getLocalizedDescription);
    }

    /**
     * Returns the dump file format description.
     * @return the dump file format description, or {@code empty} if it is not defined
     */
    public Optional<DumpFileFormat> getFileFormat() {
        return Optional.ofNullable(fileFormat);
    }

    /**
     * Builds dump options from this settings.
     * @return the built protocol buffer object
     */
    public SqlRequest.DumpOption toProtocolBuffer() {
        var builder = SqlRequest.DumpOption.newBuilder();
        if (fileFormat != null) {
            var proto = fileFormat.toProtocolBuffer();
            var descriptor = fileFormat.getFormatType().getFieldDescriptor();
            builder.setField(descriptor, proto);
        }
        // TODO: more fields
        return builder.build();
    }

    static String normalizeLanguage(String language) {
        return new Locale(language).getLanguage();
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description, localizedDescriptions, fileFormat);
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
        DumpProfile other = (DumpProfile) obj;
        return Objects.equals(title, other.title)
                && Objects.equals(description, other.description)
                && Objects.equals(localizedDescriptions, other.localizedDescriptions)
                && Objects.equals(fileFormat, other.fileFormat);
    }

    @Override
    public String toString() {
        return String.format(
                "DumpProfile(title=%s, description=%s, localizedDescriptions=%s, fileFormat=%s)", //$NON-NLS-1$
                title, description, localizedDescriptions, fileFormat);
    }


}
