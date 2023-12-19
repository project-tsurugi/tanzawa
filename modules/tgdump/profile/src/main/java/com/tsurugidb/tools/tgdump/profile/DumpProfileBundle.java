package com.tsurugidb.tools.tgdump.profile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.tgdump.core.model.DumpProfile;

/**
 * Provides built-in dump profiles.
 * @see DumpProfileBundleLoader
 */
public class DumpProfileBundle {

    private final Map<String, DumpProfile> profiles;

    /**
     * Creates a new empty instance.
     */
    public DumpProfileBundle() {
        this(Map.of());
    }

    /**
     * Creates a new instance.
     * @param profiles the member profiles
     */
    public DumpProfileBundle(@Nonnull Map<String, ? extends DumpProfile> profiles) {
        Objects.requireNonNull(profiles);
        this.profiles = adapt(profiles);
    }

    private static Map<String, DumpProfile> adapt(Map<String, ? extends DumpProfile> profiles) {
        if (profiles instanceof SortedMap<?, ?>) {
            return new TreeMap<>((SortedMap<String, ? extends DumpProfile>) profiles);
        }
        return Map.copyOf(profiles);
    }

    /**
     * Returns the available profile names in this bundle.
     * @return the available profile names
     */
    public Set<String> getProfileNames() {
        return Collections.unmodifiableSet(profiles.keySet());
    }

    /**
     * Returns an dump profile with the specified name.
     * @param name the dump profile name
     * @return the found profile
     * @throws DiagnosticException if such the profile is not found in this bundle
     */
    public DumpProfile getProfile(@Nonnull String name) throws DiagnosticException {
        return findProfile(name)
                .orElseThrow(() -> new ProfileException(ProfileDiagnosticCode.PROFILE_NOT_REGISTERED,
                        List.of(name, profiles.keySet())));
    }

    /**
     * Returns an dump profile with the specified name.
     * @param name the dump profile name
     * @return the found profile, or {@code empty} if it does not exist
     */
    public Optional<DumpProfile> findProfile(@Nonnull String name) {
        Objects.requireNonNull(name);
        return Optional.ofNullable(profiles.get(name));
    }

    @Override
    public int hashCode() {
        return Objects.hash(profiles);
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
        DumpProfileBundle other = (DumpProfileBundle) obj;
        return Objects.equals(profiles, other.profiles);
    }

    @Override
    public String toString() {
        return String.format("DumpProfileBundle%s", profiles.keySet()); //$NON-NLS-1$
    }
}
