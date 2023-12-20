package com.tsurugidb.tools.tgdump.profile;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;
import com.tsurugidb.tools.tgdump.core.model.DumpProfile;

/**
 * Loads {@link DumpProfileBundle} from the classpath.
 */
public class DumpProfileBundleLoader {

    /**
     * A bundle directory path for dump profiles.
     */
    public static final String PATH_DUMP_PROFILE_BUNDLE_DIR = "META-INF/tsurugidb/tgdump"; //$NON-NLS-1$

    /**
     * A bundle index file path.
     */
    public static final String PATH_DUMP_PROFILE_BUNDLE_INDEX =
            PATH_DUMP_PROFILE_BUNDLE_DIR + "/dump-profile.properties"; //$NON-NLS-1$


    private static final Logger LOG = LoggerFactory.getLogger(DumpProfileBundleLoader.class);

    private final DumpProfileReader profileReader;

    private final ClassLoader classLoader;

    private final boolean skipErrors;

    /**
     * Creates a new instance.
     * @param profileReader a reader for dump profile files
     * @param classLoader the class loader to retrieve dump profile files
     * @param skipErrors {@code true} if skip erroneous profiles (with logging),
     *      or {@code false} to raise an error for such the case
     */
    public DumpProfileBundleLoader(
            @Nonnull DumpProfileReader profileReader,
            @Nullable ClassLoader classLoader,
            boolean skipErrors) {
        Objects.requireNonNull(profileReader);
        this.profileReader = profileReader;
        this.classLoader = classLoader != null ? classLoader : ClassLoader.getSystemClassLoader();
        this.skipErrors = skipErrors;
    }

    /**
     * Extracts a {@link DumpProfile} from the file.
     * @param file the file path
     * @return the extracted dump file
     * @throws DiagnosticException if error occurred while loading the profile
     */
    public DumpProfile loadProfile(@Nonnull Path file) throws DiagnosticException {
        Objects.requireNonNull(file);
        return profileReader.readFromFile(file);
    }

    /**
     * Load all built-in dump profiles and returns their bundle.
     * @return the built-in dump profiles
     * @throws DiagnosticException if error was occurred while loading dump profiles
     */
    public DumpProfileBundle load() throws DiagnosticException {
        return load(PATH_DUMP_PROFILE_BUNDLE_INDEX);
    }

    /**
     * Load all dump profiles on the class-path and returns the collected bundle.
     * @param indexPath the path of the dump profile bundle index
     * @return the built-in dump profiles
     * @throws DiagnosticException if error was occurred while loading dump profiles
     */
    public DumpProfileBundle load(@Nonnull String indexPath) throws DiagnosticException {
        Objects.requireNonNull(indexPath);
        var profiles = loadProfiles(indexPath);
        return new DumpProfileBundle(profiles);
    }

    private Map<String, DumpProfile> loadProfiles(String indexPath) throws DiagnosticException {
        LOG.debug("collecting dump profile bundles: {}", indexPath); //$NON-NLS-1$
        var indexList = new ArrayList<URL>();
        try {
            classLoader.getResources(indexPath).asIterator().forEachRemaining(indexList::add);
        } catch (IOException e) {
            throw new ProfileException(ProfileDiagnosticCode.IO_ERROR,
                    List.of(indexPath, DiagnosticUtil.getMessage(e)),
                    e);
        }

        var results = new TreeMap<String, DumpProfile>(String.CASE_INSENSITIVE_ORDER);
        for (var index : indexList) {
            LOG.debug("loading dump profile bundle index: {}", index); //$NON-NLS-1$
            var properties = new Properties();
            try (var input = index.openStream()) {
                properties.load(input);
            } catch (IOException e) {
                if (skipErrors) {
                    LOG.warn("skipped wrong dump profile bundle: {}", index, e);
                    continue;
                }
                throw new ProfileException(ProfileDiagnosticCode.IO_ERROR,
                        List.of(index, DiagnosticUtil.getMessage(e)),
                        e);
            }
            for (var entry : properties.entrySet()) {
                var label = (String) entry.getKey();
                var location = (String) entry.getValue();
                try {
                    var profile = loadProfile(location);
                    var existing = results.putIfAbsent(label, profile);
                    if (existing != null && !Objects.equals(profile, existing)) {
                        LOG.warn("dump profile \"{}\" is ignored by duplication: {}", label, index);
                    }
                } catch (DiagnosticException e) {
                    if (skipErrors) {
                        LOG.warn("skipped invalid dump profile: {}", index, e);
                        continue;
                    }
                    throw e;
                }
            }
        }
        return results;
    }

    private DumpProfile loadProfile(String profilePath) throws DiagnosticException {
        var candidates = new ArrayList<URL>();
        try {
            classLoader.getResources(profilePath).asIterator().forEachRemaining(candidates::add);
        } catch (IOException e) {
            throw new ProfileException(ProfileDiagnosticCode.IO_ERROR,
                    List.of(profilePath, DiagnosticUtil.getMessage(e)),
                    e);
        }
        if (candidates.isEmpty()) {
            throw new ProfileException(ProfileDiagnosticCode.PROFILE_NOT_FOUND, List.of(profilePath));
        }
        DumpProfile result = null;
        for (var candidate : candidates) {
            var r = profileReader.readFromUrl(candidate);
            if (result == null) {
                result = r;
            } else if (!Objects.equals(result, r)) {
                LOG.warn("built-in dump profile \"{}\" is ignored by duplication", candidate);
            }
        }
        assert result != null;
        return result;
    }
}
