package com.tsurugidb.tools.tgdump.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.tgdump.core.model.ArrowFileFormat;
import com.tsurugidb.tools.tgdump.core.model.DumpProfile;
import com.tsurugidb.tools.tgdump.core.model.ParquetColumnFormat;
import com.tsurugidb.tools.tgdump.core.model.ParquetFileFormat;

class DumpProfileReaderTest {

    private final List<Path> files = new ArrayList<>();

    private final DumpProfileReader reader = new DumpProfileReader(JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build());

    @AfterEach
    void tearDown() throws Exception {
        for (var file : files) {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void simple() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "'format_version': {format_version},",
                "}",
        }));
        assertEquals(new DumpProfile(), profile);
    }

    @Test
    void title() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "'format_version': {format_version},",
                "'title': 'Hello, world!',",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withTitle("Hello, world!")
                    .build(),
                profile);
    }

    @Test
    void description() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "'format_version': {format_version},",
                "'description': 'Hello, world!',",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withDescription("Hello, world!")
                    .build(),
                profile);
    }

    @Test
    void localized_descriptions() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "'format_version': {format_version},",
                "'description.en': 'Hello, EN!',",
                "'description.ja': 'Hello, JA!',",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withLocalizedDescription("en", "Hello, EN!")
                    .withLocalizedDescription("ja", "Hello, JA!")
                    .build(),
                profile);
    }

    @Test
    void file_format_parquet() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ParquetFileFormat.newBuilder()
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_parquet_parquet_version() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'parquet_version': '2.5',",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ParquetFileFormat.newBuilder()
                            .withParquetVersion("2.5")
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_parquet_record_batch_size() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'record_batch_size': 100,",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ParquetFileFormat.newBuilder()
                            .withRecordBatchSize(100L)
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_parquet_record_batch_in_bytes() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'record_batch_in_bytes': 10000,",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ParquetFileFormat.newBuilder()
                            .withRecordBatchInBytes(10000L)
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_parquet_codec() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'codec': 'snappy',",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ParquetFileFormat.newBuilder()
                            .withCodec("snappy")
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_parquet_encoding() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'encoding': 'plain',",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ParquetFileFormat.newBuilder()
                            .withEncoding("plain")
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_parquet_columns() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'columns': [",
                "       {",
                "         'name': 'testing',",
                "       }",
                "     ],",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ParquetFileFormat.newBuilder()
                            .withColumns(List.of(
                                    ParquetColumnFormat.newBuilder("testing")
                                        .build()))
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_parquet_columns_multiple() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'columns': [",
                "       {",
                "         'name': 'a',",
                "       },",
                "       {",
                "         'name': 'b',",
                "       },",
                "       {",
                "         'name': 'c',",
                "       },",
                "     ],",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ParquetFileFormat.newBuilder()
                            .withColumns(List.of(
                                    ParquetColumnFormat.newBuilder("a")
                                        .build(),
                                    ParquetColumnFormat.newBuilder("b")
                                        .build(),
                                    ParquetColumnFormat.newBuilder("c")
                                        .build()))
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_parquet_columns_empty() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'columns': [],",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ParquetFileFormat.newBuilder()
                            .withColumns(List.of())
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_parquet_columns_codec() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'columns': [",
                "       {",
                "         'name': 'testing',",
                "         'codec': 'gzip',",
                "       }",
                "     ],",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ParquetFileFormat.newBuilder()
                            .withColumns(List.of(
                                    ParquetColumnFormat.newBuilder("testing")
                                        .withCodec("gzip")
                                        .build()))
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_parquet_columns_encoding() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'columns': [",
                "       {",
                "         'name': 'testing',",
                "         'encoding': 'plain',",
                "       }",
                "     ],",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ParquetFileFormat.newBuilder()
                            .withColumns(List.of(
                                    ParquetColumnFormat.newBuilder("testing")
                                        .withEncoding("plain")
                                        .build()))
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_arrow() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ArrowFileFormat.newBuilder()
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_arrow_metadata_version() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'metadata_version': 4,",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ArrowFileFormat.newBuilder()
                            .withMetadataVersion("4")
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_arrow_alignment() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'alignment': 16,",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ArrowFileFormat.newBuilder()
                            .withAlignment(16)
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_arrow_record_batch_size() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'record_batch_size': 100,",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ArrowFileFormat.newBuilder()
                            .withRecordBatchSize(100L)
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_arrow_record_batch_in_bytes() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'record_batch_in_bytes': 10000,",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ArrowFileFormat.newBuilder()
                            .withRecordBatchInBytes(10000L)
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_arrow_codec() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'codec': 'lz4',",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ArrowFileFormat.newBuilder()
                            .withCodec("lz4")
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_arrow_min_space_saving() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'min_space_saving': 0.95,",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ArrowFileFormat.newBuilder()
                            .withMinSpaceSaving(0.95)
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_arrow_min_space_saving_int() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'min_space_saving': 1,",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ArrowFileFormat.newBuilder()
                            .withMinSpaceSaving(1d)
                            .build())
                    .build(),
                profile);
    }

    @Test
    void file_format_arrow_character_field_type() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'character_field_type': 'STRING',",
                "  },",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .withFileFormat(ArrowFileFormat.newBuilder()
                            .withCharacterFieldType(ArrowFileFormat.CharacterFieldType.STRING)
                            .build())
                    .build(),
                profile);
    }

    @Test
    void not_a_json() throws Exception {
        var file = prepare(new String[] {
                "?",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void not_found() throws Exception {
        var file = prepare(new String[] {
                "?",
        });
        var missing = file.getParent().resolve("MISSING_" + file.getFileName());
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(missing));
        assertEquals(ProfileDiagnosticCode.PROFILE_NOT_FOUND, e.getDiagnosticCode());
    }

    @Test
    void root_not_a_object() throws Exception {
        var file = prepare(new String[] {
                "1",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void format_version_missing() throws Exception {
        var file = prepare(new String[] {
                "{}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void format_version_inconsistent() throws Exception {
        var file = prepare(new String[] {
                "{",
                "'format_version': -1,",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_UNSUPPORTED, e.getDiagnosticCode());
    }

    @Test
    void file_format_not_object() throws Exception {
        var file = prepare(new String[] {
                "{",
                "'format_version': {format_version},",
                "'file_format': 1,",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void format_type_missing() throws Exception {
        var file = prepare(new String[] {
                "{",
                "'format_version': {format_version},",
                "'file_format': {},",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void format_type_unknown() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'UNKNOWN',",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void unknown_fields() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'unknown': 1,",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void parquet_record_batch_size_not_long() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'record_batch_size': 'NG',",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void parquet_record_batch_size_zero() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'record_batch_size': 0,",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void parquet_record_batch_in_bytes_zero() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'record_batch_in_bytes': 0,",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void parquet_columns_not_array() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'columns': {},",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void parquet_columns_not_array_of_objects() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'columns': [1],",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void parquet_columns_name_missing() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'columns': [{}],",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void unknown_fields_in_parquet() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'unknown': 1,",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void unknown_fields_in_parquet_columns() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'parquet',",
                "    'columns': [{",
                "      'name': 'testing',",
                "      'unknown': 1,",
                "     }],",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void arrow_alignment_not_long() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'alignment': 'NG',",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void arrow_alignment_zero() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'alignment': 0,",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void arrow_record_batch_size_not_long() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'record_batch_size': 'NG',",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void arrow_record_batch_size_zero() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'record_batch_size': 0,",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void arrow_record_batch_in_bytes_zero() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'record_batch_in_bytes': 0,",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void arrow_min_space_saving_not_numeric() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'min_space_saving': 'NG',",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void arrow_min_space_saving_under_zero() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'min_space_saving': -0.01,",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void arrow_min_space_saving_over_one() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'min_space_saving': 1.01,",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void arrow_character_field_type_unknown() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'character_field_type': 'UNKNOWN',",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void unknown_fields_in_arrow() throws Exception {
        var file = prepare(new String[] {
                "{",
                "  'format_version': {format_version},",
                "  'file_format': {",
                "    'format_type': 'arrow',",
                "    'unknown': 1,",
                "  },",
                "}",
        });
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromFile(file));
        assertEquals(ProfileDiagnosticCode.PROFILE_INVALID, e.getDiagnosticCode());
    }

    @Test
    void nulls() throws Exception {
        var profile = reader.readFromFile(prepare(new String[] {
                "{",
                "'format_version': {format_version},",
                "'title': null,",
                "'description': null,",
                "'description.en': null,",
                "'file_format': null,",
                "}",
        }));
        assertEquals(
                DumpProfile.newBuilder()
                    .build(),
                profile);
    }

    @Test
    void from_url() throws Exception {
        var file = prepare(new String[] {
                "{",
                "'format_version': {format_version},",
                "}",
        });
        var profile = reader.readFromUrl(file.toUri().toURL());
        assertEquals(new DumpProfile(), profile);
    }

    @Test
    void from_url_not_found() throws Exception {
        var file = prepare(new String[] {
                "{",
                "'format_version': {format_version},",
                "}",
        });
        var missing = file.getParent().resolve("MISSING_" + file.getFileName());
        var e = assertThrows(DiagnosticException.class, () -> reader.readFromUrl(missing.toUri().toURL()));
        assertEquals(ProfileDiagnosticCode.PROFILE_NOT_FOUND, e.getDiagnosticCode());
    }

    private static final Map<String, String> VARS = Map.of(
            "format_version", String.valueOf(DumpProfileReader.FORMAT_VERSION));

    private static final Pattern PATTERN_VARIABLE = Pattern.compile("\\{(\\w+)\\}");

    private Path prepare(String... lines) throws IOException {
        var file = Files.createTempFile("tgdump-profile-", ".json");
        files.add(file);
        var candidate = Arrays.stream(lines)
                .map(DumpProfileReaderTest::extractVariables)
                .collect(Collectors.toList());
        Files.write(file, candidate);
        return file;
    }

    private static String extractVariables(String source) {
        // replace {<k>} -> variables.get(k)
        var results = new StringBuilder();
        var m = PATTERN_VARIABLE.matcher(source);
        int start;
        for (start = 0; m.find(start); start = m.end()) {
            results.append(source.substring(start, m.start()));
            var key = m.group(1);
            var value = VARS.get(key);
            if (value != null) {
                results.append(value);
            } else {
                throw new IllegalArgumentException(key);
            }
        }
        results.append(source.substring(start));
        return results.toString();
    }
}
