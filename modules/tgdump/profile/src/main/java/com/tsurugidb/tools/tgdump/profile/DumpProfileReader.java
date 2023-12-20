package com.tsurugidb.tools.tgdump.profile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;
import com.tsurugidb.tools.tgdump.core.model.ArrowFileFormat;
import com.tsurugidb.tools.tgdump.core.model.DumpFileFormat;
import com.tsurugidb.tools.tgdump.core.model.DumpProfile;
import com.tsurugidb.tools.tgdump.core.model.ParquetFileFormat;

/**
 * Extracts dump profile from files.
 */
public class DumpProfileReader {

    /**
     * The dump profile file format version.
     */
    public static final int FORMAT_VERSION = 1;

    /**
     * The JSON field name of the file format version.
     */
    public static final String FIELD_FORMAT_VERSION = "format_version"; //$NON-NLS-1$

    /**
     * The JSON field name of the file format version.
     */
    public static final String FIELD_TITLE = "title"; //$NON-NLS-1$

    /**
     * The JSON field name of the file format version.
     */
    public static final String FIELD_DESCRIPTION = "description"; //$NON-NLS-1$

    /**
     * The JSON field name of the file format version.
     */
    public static final String FIELD_PREFIX_LOCALIZED_DESCRIPTION = "description."; //$NON-NLS-1$

    /**
     * The JSON field name of the file format version.
     */
    public static final String FIELD_FILE_FORMAT = "file_format"; //$NON-NLS-1$

    /**
     * The JSON field name of the file format type name.
     */
    public static final String FIELD_FORMAT_TYPE = "format_type"; //$NON-NLS-1$

    /**
     * The dump file format name of Apache Parquet.
     */
    public static final String FORMAT_TYPE_PARQUET = "parquet"; //$NON-NLS-1$

    /**
     * The dump file format name of Apache Arrow.
     */
    public static final String FORMAT_TYPE_ARROW = "arrow"; //$NON-NLS-1$

    static final Logger LOG = LoggerFactory.getLogger(DumpProfileReader.class);

    private final ObjectMapper objectMapper;

    /**
     * Creates a new instance with recommended settings.
     */
    public DumpProfileReader() {
        this(JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
                .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
                .build());
    }

    /**
     * Creates a new instance.
     * @param objectMapper the JSON parser factory
     */
    public DumpProfileReader(@Nonnull ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper);
        this.objectMapper = objectMapper;
    }

    /**
     * Extracts a {@link DumpProfile} from the file.
     * @param path the file path
     * @return the extracted dump file
     * @throws DiagnosticException if error occurred while loading the profile
     */
    public DumpProfile readFromFile(@Nonnull Path path) throws DiagnosticException {
        Objects.requireNonNull(path);
        LOG.trace("enter: loadFromFile: {}", path); //$NON-NLS-1$
        JsonNode node;
        try {
            LOG.debug("loading JSON file: {}", path); //$NON-NLS-1$
            node = objectMapper.readTree(path.toFile());
        } catch (FileNotFoundException e) {
            LOG.debug("exception was occurred in loadFromFile", e); //$NON-NLS-1$
            throw new ProfileException(ProfileDiagnosticCode.PROFILE_NOT_FOUND, List.of(path));
        } catch (JsonParseException e) {
            LOG.debug("exception was occurred in loadFromFile", e); //$NON-NLS-1$
            throw new ProfileException(ProfileDiagnosticCode.PROFILE_INVALID, List.of(path, DiagnosticUtil.getMessage(e)), e);
        } catch (IOException e) {
            LOG.debug("exception was occurred in loadFromFile", e); //$NON-NLS-1$
            throw new ProfileException(ProfileDiagnosticCode.IO_ERROR, List.of(path, DiagnosticUtil.getMessage(e)), e);
        }
        var result = convert(path.toString(), node);
        LOG.trace("exit: loadFromFile: {} -> {}", path, result); //$NON-NLS-1$
        return result;
    }

    /**
     * Extracts a {@link DumpProfile} from the URL.
     * @param url the URL
     * @return the extracted dump file
     * @throws DiagnosticException if error occurred while loading the profile
     */
    public DumpProfile readFromUrl(@Nonnull URL url) throws DiagnosticException {
        Objects.requireNonNull(url);
        LOG.trace("enter: loadFromUrl: {}", url); //$NON-NLS-1$
        JsonNode node;
        try {
            LOG.debug("loading JSON file: {}", url); //$NON-NLS-1$
            node = objectMapper.readTree(url);
        } catch (FileNotFoundException e) {
            LOG.debug("exception was occurred in loadFromUrl", e); //$NON-NLS-1$
            throw new ProfileException(ProfileDiagnosticCode.PROFILE_NOT_FOUND, List.of(url));
        } catch (JsonParseException e) {
            LOG.debug("exception was occurred in loadFromUrl", e); //$NON-NLS-1$
            throw new ProfileException(ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(url, DiagnosticUtil.getMessage(e)),
                    e);
        } catch (IOException e) {
            LOG.debug("exception was occurred in loadFromUrl", e); //$NON-NLS-1$
            throw new ProfileException(ProfileDiagnosticCode.IO_ERROR,
                    List.of(url, DiagnosticUtil.getMessage(e)),
                    e);
        }
        var result = convert(url.toString(), node);
        LOG.trace("exit: loadFromUrl: {} -> {}", url, result); //$NON-NLS-1$
        return result;
    }

    private static DumpProfile convert(String location, JsonNode node) throws DiagnosticException {
        LOG.trace("converting dump profile: {} - {}", location, node); //$NON-NLS-1$
        var consumed = new HashSet<String>();

        var profile = DumpProfile.newBuilder();
        if (!node.isObject()) {
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, "root must be an object")); //$NON-NLS-1$
        }
        // format version
        var formatVersion = findNode(consumed, node, FIELD_FORMAT_VERSION)
                .filter(it -> it.isInt())
                .orElseThrow(() -> new ProfileException(
                        ProfileDiagnosticCode.PROFILE_INVALID,
                        List.of(location, "format version is absent"))); //$NON-NLS-1$
        if (formatVersion.asInt() != FORMAT_VERSION) {
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_UNSUPPORTED,
                    List.of(location, formatVersion.asInt(), FORMAT_VERSION));
        }

        // title
        findString(consumed, node, FIELD_TITLE)
            .filter(it -> !it.isBlank())
            .ifPresent(profile::withTitle);

        // description
        findString(consumed, node, FIELD_DESCRIPTION)
            .filter(it -> !it.isBlank())
            .ifPresent(profile::withDescription);

        // localized descriptions
        for (var iter = node.fields(); iter.hasNext();) {
            var pair = iter.next();
            var fieldName = pair.getKey();
            var fieldNode = pair.getValue();
            if (fieldName.length() > FIELD_PREFIX_LOCALIZED_DESCRIPTION.length()
                    && fieldName.startsWith(FIELD_PREFIX_LOCALIZED_DESCRIPTION)) {
                consumed.add(fieldName);
                var language = fieldName.substring(FIELD_PREFIX_LOCALIZED_DESCRIPTION.length());
                if (!fieldNode.isNull()) {
                    profile.withLocalizedDescription(language, fieldNode.asText());
                }
            }
        }

        // file format
        var fileFormat = findNode(consumed, node, FIELD_FILE_FORMAT).orElse(null);
        if (fileFormat != null && !fileFormat.isObject()) {
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, "file format description must be an object"));
        }
        if (fileFormat != null) {
            var ff = convertFileFormat(location, fileFormat);
            profile.withFileFormat(ff);
        }

        // check extra fields
        checkExtraFields(location, node, "", consumed); //$NON-NLS-1$
        return profile.build();
    }

    private static DumpFileFormat convertFileFormat(String location, JsonNode node) throws DiagnosticException {
        LOG.trace("converting dump file format: {} - {}", location, node); //$NON-NLS-1$
        // format_type
        var formatType = findString(new ArrayList<>(), node, FIELD_FORMAT_TYPE)
            .filter(it -> !it.isBlank())
            .map(it -> it.toLowerCase(Locale.ENGLISH))
            .orElseThrow(() -> new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, MessageFormat.format(
                            "dump file format type must be set: {0}.{1}",
                            FIELD_FILE_FORMAT,
                            FIELD_FORMAT_TYPE))));

        switch (formatType) {
        case FORMAT_TYPE_PARQUET:
            return convertParquetFileFormat(location, node);
        case FORMAT_TYPE_ARROW:
            return convertArrowFileFormat(location, node);
        default:
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, MessageFormat.format(
                            "unsupported dump file format type: {0}.{1}={2}",
                            FIELD_FILE_FORMAT,
                            FIELD_FORMAT_TYPE,
                            formatType)));
        }
    }

    private static ParquetFileFormat convertParquetFileFormat(String location, JsonNode node)
            throws DiagnosticException {
        var prefix = FIELD_FILE_FORMAT + '.';
        var consumed = new HashSet<String>();
        consumed.add(FIELD_FORMAT_TYPE); // already consumed
        var result = ParquetFileFormat.newBuilder();

        // more fields

        checkExtraFields(location, node, prefix, consumed);
        return result.build();
    }

    private static ArrowFileFormat convertArrowFileFormat(String location, JsonNode node)
            throws DiagnosticException {
        var prefix = FIELD_FILE_FORMAT + '.';
        var consumed = new HashSet<String>();
        consumed.add(FIELD_FORMAT_TYPE); // already consumed
        var result = ArrowFileFormat.newBuilder();

        // more fields

        checkExtraFields(location, node, prefix, consumed);
        return result.build();
    }


    private static Optional<JsonNode> findNode(Collection<String> consumed, JsonNode node, String fieldName) {
        if (consumed != null) {
            consumed.add(fieldName);
        }
        return Optional.ofNullable(node.get(fieldName)).filter(it -> !it.isNull());
    }

    private static Optional<String> findString(Collection<String> consumed, JsonNode node, String fieldName) {
        if (consumed != null) {
            consumed.add(fieldName);
        }
        return findNode(consumed, node, fieldName).map(JsonNode::asText);
    }

    private static void checkExtraFields(String location, JsonNode node, String prefix, Set<String> consumed)
            throws DiagnosticException {
        for (var iter = node.fieldNames(); iter.hasNext();) {
            var fieldName = iter.next();
            if (consumed.contains(fieldName)) {
                continue;
            }
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, MessageFormat.format(
                            "unrecognized field: {0}",
                            prefix + fieldName)));
        }
    }
}
