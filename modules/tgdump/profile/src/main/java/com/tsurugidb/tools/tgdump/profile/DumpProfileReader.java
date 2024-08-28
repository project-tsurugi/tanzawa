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
package com.tsurugidb.tools.tgdump.profile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.tsurugidb.tools.tgdump.core.model.ParquetColumnFormat;
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
     * @see DumpProfile#getTitle()
     */
    public static final String FIELD_TITLE = "title"; //$NON-NLS-1$

    /**
     * The JSON field name of the file format version.
     * @see DumpProfile#getDescription()
     */
    public static final String FIELD_DESCRIPTION = "description"; //$NON-NLS-1$

    /**
     * The JSON field name of the file format version.
     * @see DumpProfile#getLocalizedDescription(Locale)
     */
    public static final String FIELD_PREFIX_LOCALIZED_DESCRIPTION = "description."; //$NON-NLS-1$

    /**
     * The JSON field name of the file format version.
     * @see DumpProfile#getFileFormat()
     */
    public static final String FIELD_FILE_FORMAT = "file_format"; //$NON-NLS-1$

    /**
     * The JSON field name of the file format type name.
     * @see DumpFileFormat#getFormatType()
     */
    public static final String FIELD_FORMAT_TYPE = "format_type"; //$NON-NLS-1$

    /**
     * The dump file format name of Apache Parquet.
     * @see com.tsurugidb.tools.tgdump.core.model.DumpFileFormat.FormatType#PARQUET
     */
    public static final String FORMAT_TYPE_PARQUET = "parquet"; //$NON-NLS-1$

    /**
     * The dump file format name of Apache Arrow.
     * @see com.tsurugidb.tools.tgdump.core.model.DumpFileFormat.FormatType#ARROW
     */
    public static final String FORMAT_TYPE_ARROW = "arrow"; //$NON-NLS-1$

    /**
     * The JSON field name of Parquet version.
     * @see ParquetFileFormat#getParquetVersion()
     */
    public static final String FIELD_PARQUET_VERSION = "parquet_version"; //$NON-NLS-1$

    /**
     * The JSON field name of record batch size.
     * @see ParquetFileFormat#getRecordBatchSize()
     * @see ArrowFileFormat#getRecordBatchSize()
     */
    public static final String FIELD_RECORD_BATCH_SIZE = "record_batch_size"; //$NON-NLS-1$

    /**
     * The JSON field name of record batch size from estimated record size.
     * @see ParquetFileFormat#getRecordBatchInBytes()
     * @see ArrowFileFormat#getRecordBatchInBytes()
     */
    public static final String FIELD_RECORD_BATCH_IN_BYTES = "record_batch_in_bytes"; //$NON-NLS-1$

    /**
     * The JSON field name of compression codec.
     * @see ParquetFileFormat#getCodec()
     * @see ParquetColumnFormat#getCodec()
     * @see ArrowFileFormat#getCodec()
     */
    public static final String FIELD_CODEC = "codec"; //$NON-NLS-1$

    /**
     * The JSON field name of column encoding.
     * @see ParquetFileFormat#getEncoding()
     * @see ParquetColumnFormat#getEncoding()
     */
    public static final String FIELD_ENCODING = "encoding"; //$NON-NLS-1$

    /**
     * The JSON field name of column specific settings.
     * @see ParquetFileFormat#getColumns()
     */
    public static final String FIELD_COLUMNS = "columns"; //$NON-NLS-1$

    /**
     * The JSON field name of metadata version.
     * @see ArrowFileFormat#getMetadataVersion()
     */
    public static final String FIELD_METADATA_VERSION = "metadata_version"; //$NON-NLS-1$

    /**
     * The JSON field name of byte alignment.
     * @see ArrowFileFormat#getAlignment()
     */
    public static final String FIELD_ALIGNMENT = "alignment"; //$NON-NLS-1$

    /**
     * The JSON field name of threshold for adopting compressed data.
     * @see ArrowFileFormat#getMinSpaceSaving()
     */
    public static final String FIELD_MIN_SPACE_SAVING = "min_space_saving"; //$NON-NLS-1$

    /**
     * The JSON field name of {@code CHAR} field type in Apache Arrow format.
     * @see ArrowFileFormat#getCharacterFieldType()
     * @see com.tsurugidb.tools.tgdump.core.model.ArrowFileFormat.CharacterFieldType
     */
    public static final String FIELD_CHARACTER_FIELD_TYPE = "character_field_type"; //$NON-NLS-1$

    /**
     * The JSON field name of target column name.
     * @see ParquetColumnFormat#getName()
     */
    public static final String FIELD_NAME = "name"; //$NON-NLS-1$

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

        findString(consumed, node, FIELD_PARQUET_VERSION)
            .filter(it -> !it.isBlank())
            .ifPresent(result::withParquetVersion);

        checkPositiveLong(
                findNode(consumed, node, FIELD_RECORD_BATCH_SIZE),
                location, prefix + FIELD_RECORD_BATCH_SIZE)
                .ifPresent(result::withRecordBatchSize);

        checkPositiveLong(
                findNode(consumed, node, FIELD_RECORD_BATCH_IN_BYTES),
                location, prefix + FIELD_RECORD_BATCH_IN_BYTES)
                .ifPresent(result::withRecordBatchInBytes);

        findString(consumed, node, FIELD_CODEC)
            .filter(it -> !it.isBlank())
            .ifPresent(result::withCodec);

        findString(consumed, node, FIELD_ENCODING)
            .filter(it -> !it.isBlank())
            .ifPresent(result::withEncoding);

        var columns = findNode(consumed, node, FIELD_COLUMNS);
        if (columns.isPresent()) {
            if (!columns.get().isArray()) {
                throw new ProfileException(
                        ProfileDiagnosticCode.PROFILE_INVALID,
                        List.of(location, MessageFormat.format(
                                "value must be an array of objects: {0}",
                                prefix + FIELD_COLUMNS)));
            }
            var columnList = new ArrayList<ParquetColumnFormat>(columns.get().size());
            for (var element : columns.get()) {
                if (!element.isObject()) {
                    throw new ProfileException(
                            ProfileDiagnosticCode.PROFILE_INVALID,
                            List.of(location, MessageFormat.format(
                                    "value must be an object: {0}[{1}]",
                                    prefix + FIELD_COLUMNS,
                                    columnList.size())));
                }
                var r = convertParquetColumnFormat(location, columnList.size(), element);
                columnList.add(r);
            }
            result.withColumns(columnList);
        }

        checkExtraFields(location, node, prefix, consumed);
        return result.build();
    }

    private static ParquetColumnFormat convertParquetColumnFormat(String location, int index, JsonNode node)
            throws DiagnosticException {
        var prefix = String.format("%s.%s[%d].", FIELD_FILE_FORMAT, FIELD_COLUMNS, index); //$NON-NLS-1$
        var consumed = new HashSet<String>();

        var name = findString(consumed, node, FIELD_NAME)
                .filter(it -> !it.isBlank())
                .map(it -> it.toLowerCase(Locale.ENGLISH))
                .orElseThrow(() -> new ProfileException(
                        ProfileDiagnosticCode.PROFILE_INVALID,
                        List.of(location, MessageFormat.format(
                                "target column name must be set: {0}",
                                prefix + FIELD_NAME))));

        var result = ParquetColumnFormat.forColumn(name);

        findString(consumed, node, FIELD_CODEC)
            .filter(it -> !it.isBlank())
            .ifPresent(result::withCodec);

        findString(consumed, node, FIELD_ENCODING)
            .filter(it -> !it.isBlank())
            .ifPresent(result::withEncoding);

        checkExtraFields(location, node, prefix, consumed);
        return result.build();
    }

    private static ArrowFileFormat convertArrowFileFormat(String location, JsonNode node)
            throws DiagnosticException {
        var prefix = FIELD_FILE_FORMAT + '.';
        var consumed = new HashSet<String>();
        consumed.add(FIELD_FORMAT_TYPE); // already consumed
        var result = ArrowFileFormat.newBuilder();


        findString(consumed, node, FIELD_METADATA_VERSION)
            .filter(it -> !it.isBlank())
            .ifPresent(result::withMetadataVersion);

        checkPositiveInt(
                findNode(consumed, node, FIELD_ALIGNMENT),
                location, prefix + FIELD_ALIGNMENT)
                .ifPresent(result::withAlignment);

        checkPositiveLong(
                findNode(consumed, node, FIELD_RECORD_BATCH_SIZE),
                location, prefix + FIELD_RECORD_BATCH_SIZE)
                .ifPresent(result::withRecordBatchSize);

        checkPositiveLong(
                findNode(consumed, node, FIELD_RECORD_BATCH_IN_BYTES),
                location, prefix + FIELD_RECORD_BATCH_IN_BYTES)
                .ifPresent(result::withRecordBatchInBytes);

        findString(consumed, node, FIELD_CODEC)
            .filter(it -> !it.isBlank())
            .ifPresent(result::withCodec);

        checkNomarizedRatio(
                findNode(consumed, node, FIELD_MIN_SPACE_SAVING),
                location, prefix + FIELD_MIN_SPACE_SAVING)
                .ifPresent(result::withMinSpaceSaving);

        checkEnum(
                findNode(consumed, node, FIELD_CHARACTER_FIELD_TYPE),
                ArrowFileFormat.CharacterFieldType.class,
                location, prefix + FIELD_CHARACTER_FIELD_TYPE)
                .ifPresent(result::withCharacterFieldType);

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

    private static OptionalInt checkPositiveInt(
            Optional<JsonNode> node, String location, String path) throws ProfileException {
        if (node.isEmpty()) {
            return OptionalInt.empty();
        }
        var n = node.get();
        if (!n.canConvertToInt()) {
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, MessageFormat.format(
                            "value must be an int value: {0} ({1})",
                            path,
                            n.asText())));
        }
        var value = n.asInt();
        if (value < 1) {
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, MessageFormat.format(
                            "value must be greater than 0: {0} ({1})",
                            path,
                            value)));
        }
        return OptionalInt.of(value);
    }

    private static OptionalLong checkPositiveLong(
            Optional<JsonNode> node, String location, String path) throws ProfileException {
        if (node.isEmpty()) {
            return OptionalLong.empty();
        }
        var n = node.get();
        if (!n.canConvertToLong()) {
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, MessageFormat.format(
                            "value must be a long value: {0} ({1})",
                            path,
                            n.asText())));
        }
        var value = n.asLong();
        if (value < 1) {
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, MessageFormat.format(
                            "value must be greater than 0: {0} ({1})",
                            path,
                            value)));
        }
        return OptionalLong.of(value);
    }

    private static OptionalDouble checkNomarizedRatio(
            Optional<JsonNode> node, String location, String path) throws ProfileException {
        if (node.isEmpty()) {
            return OptionalDouble.empty();
        }
        var n = node.get();
        if (!n.isNumber()) {
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, MessageFormat.format(
                            "value must be a numeric value: {0} ({1})",
                            path,
                            n.asText())));
        }
        var value = n.asDouble();
        if (value < 0.0 || value > 1.0) {
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, MessageFormat.format(
                            "value must be in [0.0, 1.0]: {0} ({1})",
                            path,
                            value)));
        }
        return OptionalDouble.of(value);
    }

    private static <E extends Enum<E>> Optional<E> checkEnum(
            Optional<JsonNode> node, Class<E> enumType, String location, String path) throws ProfileException {
        var name = node.map(JsonNode::asText)
                .map(String::strip)
                .filter(it -> !it.isEmpty())
                .map(it -> it.toUpperCase(Locale.ENGLISH))
                .orElse(null);
        if (name == null) {
            return Optional.empty();
        }
        try {
            var constant = Enum.valueOf(enumType, name);
            return Optional.of(constant);
        } catch (IllegalArgumentException e) {
            LOG.debug("cannot find enum constant: {}#{}", enumType.getName(), name, e); //$NON-NLS-1$
            throw new ProfileException(
                    ProfileDiagnosticCode.PROFILE_INVALID,
                    List.of(location, MessageFormat.format(
                            "unrecognized value \"{1}\" in {0}, must be one of {2}",
                            path,
                            name,
                            Arrays.stream(enumType.getEnumConstants())
                                    .map(Enum::name)
                                    .collect(Collectors.joining(", ", "{", "}"))))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
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
