package com.tsurugidb.tools.common.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides version information of Tsurugi libraries.
 * @see #loadByName(String, ClassLoader)
 */
public class LibraryVersion {

    /**
     * Path to the library configuration file directory.
     */
    public static final String PATH_LIBRARY_CONFIGURATION_BASE = "META-INF/tsurugidb/";

    /**
     * File name suffix of the library configuration files.
     */
    public static final String PATH_LIBRARY_CONFIGURATION_SUFFIX = ".properties";

    /**
     * Property key of library version.
     */
    public static final String KEY_BUILD_VERSION = "Build-Version";

    /**
     * Property key of library revision.
     */
    public static final String KEY_BUILD_REVISION = "Build-Revision";

    /**
     * Property key of library build timestamp.
     */
    public static final String KEY_BUILD_TIMESTAMP = "Build-Timestamp";

    /**
     * Property key of library build OS.
     */
    public static final String KEY_BUILD_OS = "Build-OS";

    /**
     * Property key of library build JDK.
     */
    public static final String KEY_BUILD_JDK = "Build-Jdk";

    private static final DateTimeFormatter TIMESTAMP_FORMAT = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_DATE_TIME)
            .appendOffset("+HHmm", "Z") //$NON-NLS-1$ //$NON-NLS-2$
            .toFormatter(Locale.ENGLISH);

    private static final Logger LOG = LoggerFactory.getLogger(LibraryVersion.class);

    private final @Nullable String buildVersion;

    private final @Nullable String buildRevision;

    private final @Nullable OffsetDateTime buildTimestamp;

    private final @Nullable String buildOs;

    private final @Nullable String buildJdk;

    /**
     * Creates a new instance.
     * @param buildVersion the library version
     * @param buildRevision the library revision
     * @param buildTimestamp the build timestamp
     * @param buildOs the build OS
     * @param buildJdk the build JDK
     */
    public LibraryVersion(
            @Nullable String buildVersion,
            @Nullable String buildRevision,
            @Nullable OffsetDateTime buildTimestamp,
            @Nullable String buildOs,
            @Nullable String buildJdk) {
        this.buildVersion = buildVersion;
        this.buildRevision = buildRevision;
        this.buildTimestamp = buildTimestamp;
        this.buildOs = buildOs;
        this.buildJdk = buildJdk;
    }

    /**
     * Loads the library configuration with the specified name on the classpath.
     * @param name the library name
     * @param loader the class loader to retrieve the configuration file
     * @return the loaded library configuration
     * @throws FileNotFoundException if such the configuration file is not found
     * @throws IOException if I/O error was occurred while loading the library configuration
     * @see #loadByName(String, ClassLoader)
     */
    public static LibraryVersion loadByName(@Nonnull String name, @Nullable ClassLoader loader) throws IOException {
        Objects.requireNonNull(name);
        return loadFromPath(PATH_LIBRARY_CONFIGURATION_BASE + name + PATH_LIBRARY_CONFIGURATION_SUFFIX, loader);

    }

    /**
     * Loads the library configuration from the path on the classpath.
     * @param path the library configuration file path
     * @param loader the class loader to retrieve the configuration file
     * @return the loaded library configuration
     * @throws FileNotFoundException if such the configuration file is not found
     * @throws IOException if I/O error was occurred while loading the library configuration
     * @see #loadFromPath(String, ClassLoader)
     */
    public static LibraryVersion loadFromPath(@Nonnull String path, @Nullable ClassLoader loader) throws IOException {
        Objects.requireNonNull(path);
        return load0(path, loader != null ? loader : ClassLoader.getSystemClassLoader());
    }

    private static LibraryVersion load0(String path, ClassLoader loader) throws IOException {
        LOG.debug("loading library configuration: {}", path); //$NON-NLS-1$
        var resource = loader.getResource(path);
        if (resource == null) {
            throw new FileNotFoundException(MessageFormat.format(
                    "library configuration file is not found: {0}",
                    path));
        }
        LOG.debug("found library configuration: {} -> {}", path, resource); //$NON-NLS-1$
        Properties properties = new Properties();
        try (var input = resource.openStream()) {
            properties.load(input);
        }

        var result = new LibraryVersion(
                get(properties, KEY_BUILD_VERSION).orElse(null),
                get(properties, KEY_BUILD_REVISION).orElse(null),
                get(properties, KEY_BUILD_TIMESTAMP)
                    .flatMap(it -> {
                        try {
                            return Optional.of(OffsetDateTime.parse(it, TIMESTAMP_FORMAT));
                        } catch (DateTimeParseException e) {
                            LOG.warn("failed to parse timestamp of library configuration: {} ({})", it, resource, e);
                            return Optional.empty();
                        }
                    })
                    .orElse(null),
                get(properties, KEY_BUILD_OS).orElse(null),
                get(properties, KEY_BUILD_JDK).orElse(null));

        LOG.debug("library configuration: {} -> {}", path, result); //$NON-NLS-1$
        return result;
    }

    private static Optional<String> get(Properties properties, String key) {
        return Optional.ofNullable(properties.getProperty(key))
                .map(String::strip)
                .filter(it -> !it.isEmpty());
    }

    /**
     * Returns the library version.
     * @return the library version
     */
    public Optional<String> getBuildVersion() {
        return Optional.ofNullable(buildVersion);
    }

    /**
     * Returns the library revision.
     * @return the library revision
     */
    public Optional<String> getBuildRevision() {
        return Optional.ofNullable(buildRevision);
    }

    /**
     * Return the timestamp when this library was built.
     * @return the build timestamp
     */
    public Optional<OffsetDateTime> getBuildTimestamp() {
        return Optional.ofNullable(buildTimestamp);
    }

    /**
     * Returns the OS name which builds the library.
     * @return the build OS
     */
    public Optional<String> getBuildOs() {
        return Optional.ofNullable(buildOs);
    }

    /**
     * Returns the JDK name which builds the library.
     * @return the build JDK
     */
    public Optional<String> getBuildJdk() {
        return Optional.ofNullable(buildJdk);
    }

    @Override
    public int hashCode() {
        return Objects.hash(buildVersion, buildRevision, buildTimestamp, buildOs, buildJdk);
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
        LibraryVersion other = (LibraryVersion) obj;
        return Objects.equals(buildVersion, other.buildVersion)
                && Objects.equals(buildRevision, other.buildRevision)
                && Objects.equals(buildTimestamp, other.buildTimestamp)
                && Objects.equals(buildOs, other.buildOs)
                && Objects.equals(buildJdk, other.buildJdk);
    }

    @Override
    public String toString() {
        return String.format(
                "LibraryVersion(buildVersion=%s, buildRevision=%s, buildTimestamp=%s, buildOs=%s, buildJdk=%s)", //$NON-NLS-1$
                buildVersion, buildRevision, buildTimestamp, buildOs, buildJdk);
    }
}
