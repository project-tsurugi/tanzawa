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
package com.tsurugidb.tools.common.monitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.value.Array;
import com.tsurugidb.tools.common.value.Property;
import com.tsurugidb.tools.common.value.Record;
import com.tsurugidb.tools.common.value.Value;

/**
 * A {@link Monitor} that output monitoring information as JSON Lines.
 */
public class JsonMonitor implements Monitor {

    /**
     * A terminating writer interface of {@link JsonMonitor}.
     */
    interface Delegate extends AutoCloseable {

        /**
         * Output the record into target.
         * @param record the record to output
         * @throws MonitoringException if failed to output the record
         */
        void write(@Nonnull Record record) throws MonitoringException;

        /**
         * Close the underlying outputs.
         * @throws IOException if I/O error was occurred
         */
        @Override
        void close() throws IOException;
    }

    /**
     * An implementation of {@link Delegate} that write records to file as JSON Lines.
     */
    static class JsonOutput implements Delegate {

        private final Path output;

        private final JsonGenerator generator;

        private final AtomicBoolean closed = new AtomicBoolean(false);

        private final AtomicBoolean sawError = new AtomicBoolean(false);

        /**
         * Creates a new instance.
         * @param factory the JSON factory
         * @param output the output path
         * @throws IOException if I/O error was occurred while opening the output file
         */
        JsonOutput(@Nonnull JsonFactory factory, @Nonnull Path output) throws IOException {
            Objects.requireNonNull(factory);
            Objects.requireNonNull(output);
            this.output = output.toAbsolutePath();
            var parent = this.output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            this.generator = factory.createGenerator(output.toFile(), JsonEncoding.UTF8)
                    .setPrettyPrinter(new MinimalPrettyPrinter(System.lineSeparator()));
        }

        @Override
        public void write(@Nonnull Record record) throws MonitoringException {
            Objects.requireNonNull(record);
            if (closed.get()) {
                throw new IllegalStateException(MessageFormat.format(
                        "JSON monitor is already closed: {0}",
                        output));
            }
            if (sawError.get()) {
                LOG.warn("JSON monitor: {}", record); //$NON-NLS-1$
                return;
            }
            try {
                writeRecord(record);
                generator.flush();
            } catch (IOException e) {
                sawError.set(true);
                LOG.warn("JSON monitor: {}", record); //$NON-NLS-1$
                throw new MonitoringException(MonitoringDiagnosticCode.OUTPUT_ERROR, List.of(output), e);
            }
        }

        private void writeValue(Value value) throws IOException {
            switch (value.getKind()) {
            case NULL:
                generator.writeNull();
                return;
            case BOOLEAN:
                generator.writeBoolean(value.asBoolean());
                return;
            case INTEGER:
                generator.writeNumber(value.asInteger());
                return;
            case DECIMAL:
                generator.writeNumber(value.asDecimal());
                return;
            case STRING:
                generator.writeString(value.asString());
                return;
            case ARRAY:
                writeArray(value.asArray());
                return;
            case RECORD:
                writeRecord(value.asRecord());
                return;
            }
            throw new AssertionError(value.getKind());
        }

        private void writeArray(Array array) throws IOException {
            generator.writeStartArray();
            for (var element : array.getElements()) {
                writeValue(element);
            }
            generator.writeEndArray();
        }

        private void writeRecord(Record record) throws IOException {
            generator.writeStartObject();
            for (var property : record.getProperties()) {
                generator.writeFieldName(property.getName());
                writeValue(property.getValue());
            }
            generator.writeEndObject();
        }

        @Override
        public void close() throws IOException {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            generator.close();
        }

        @Override
        public String toString() {
            return String.valueOf(output);
        }
    }

    /**
     * The JSON field name of record kind.
     */
    public static final String FIELD_KIND = "kind"; //$NON-NLS-1$

    /**
     * The JSON field name of time-stamp.
     */
    public static final String FIELD_TIMESTAMP = "timestamp"; //$NON-NLS-1$

    /**
     * The JSON field name of format for data record.
     */
    public static final String FIELD_DATA_FORMAT = "format"; //$NON-NLS-1$

    /**
     * The JSON field name of status for finish record.
     */
    public static final String FIELD_FINISH_STATUS = "status"; //$NON-NLS-1$

    /**
     * The JSON field name of failure reason for finish record.
     */
    public static final String FIELD_FINISH_REASON = "reason"; //$NON-NLS-1$

    /**
     * The JSON field name of failure cause for finish record.
     */
    public static final String FIELD_FINISH_CAUSE = "cause"; //$NON-NLS-1$

    /**
     * The JSON field name of messages.
     */
    public static final String FIELD_MESSAGE = "message"; //$NON-NLS-1$

    /**
     * The JSON field name of message code.
     */
    public static final String FIELD_CODE = "code"; //$NON-NLS-1$

    /**
     * The JSON field name of arguments for messages.
     */
    public static final String FIELD_ARGUMENTS = "arguments"; //$NON-NLS-1$

    /**
     * The success status code of {@link #FIELD_FINISH_STATUS}.
     */
    public static final String STATUS_SUCCESS = "success"; //$NON-NLS-1$

    /**
     * The failure status code of {@link #FIELD_FINISH_STATUS}.
     */
    public static final String STATUS_FAILURE = "failure"; //$NON-NLS-1$

    /**
     * The record kind of starting application.
     */
    public static final String KIND_START = "start"; //$NON-NLS-1$

    /**
     * The record kind of finishing application.
     */
    public static final String KIND_FINISH = "finish"; //$NON-NLS-1$

    /**
     * The record kind of application data.
     */
    public static final String KIND_DATA = "data"; //$NON-NLS-1$

    static final Logger LOG = LoggerFactory.getLogger(JsonMonitor.class);

    private final Delegate delegate;

    /**
     * Creates a new instance.
     * @param output the output path
     * @throws IOException if I/O error was occurred while opening the output file
     */
    public JsonMonitor(@Nonnull Path output) throws IOException {
        this(new JsonOutput(new JsonFactory(), output));
    }

    /**
     * Creates a new instance.
     * @param factory the JSON factory
     * @param output the output path
     * @throws IOException if I/O error was occurred while opening the output file
     */
    public JsonMonitor(@Nonnull JsonFactory factory, @Nonnull Path output) throws IOException {
        this(new JsonOutput(factory, output));
    }

    /**
     * Creates a new instance.
     * @param delegate the writer delegate
     */
    JsonMonitor(@Nonnull Delegate delegate) {
        Objects.requireNonNull(delegate);
        this.delegate = delegate;
    }

    @Override
    public void onStart() throws MonitoringException {
        delegate.write(Record.of(kind(KIND_START), timestamp()));
    }

    @Override
    public void onData(String format, List<? extends Property> properties) throws MonitoringException {
        Objects.requireNonNull(format);
        Objects.requireNonNull(properties);
        var results = new ArrayList<Property>(properties.size() + 3);
        results.add(kind(KIND_DATA));
        results.add(timestamp());
        results.add(Property.of(FIELD_DATA_FORMAT, Value.of(format)));
        results.addAll(properties);
        delegate.write(new Record(results));
    }

    @Override
    public void onSuccess() throws MonitoringException {
        delegate.write(Record.of(
                kind(KIND_FINISH),
                timestamp(),
                Property.of(FIELD_FINISH_STATUS, Value.of(STATUS_SUCCESS))));
    }

    @Override
    public void onFailure(@Nullable Throwable cause, @Nonnull DiagnosticCode code, @Nonnull List<?> arguments)
            throws MonitoringException {
        Objects.requireNonNull(code);
        Objects.requireNonNull(arguments);
        // TODO: i18n diagnostic messages
        delegate.write(Record.of(
                kind(KIND_FINISH),
                timestamp(),
                Property.of(FIELD_FINISH_STATUS, Value.of(STATUS_FAILURE)),
                Property.of(FIELD_FINISH_REASON, Value.of(code.getTag())),
                Property.of(FIELD_FINISH_CAUSE, extractCause(cause)),
                Property.of(FIELD_MESSAGE, Value.of(code.getMessage(arguments)))));
    }

    @Override
    public void onFailure(DiagnosticException exception) throws MonitoringException {
        Objects.requireNonNull(exception);
        onFailure(exception.getCause(), exception.getDiagnosticCode(), exception.getArguments());
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public String toString() {
        return String.format("JsonMonitor(%s)", delegate); //$NON-NLS-1$
    }

    private static Property kind(String kind) {
        return Property.of(FIELD_KIND, Value.of(kind));
    }

    private static Property timestamp() {
        return Property.of(
                FIELD_TIMESTAMP,
                Value.of(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
    }

    private static Value extractCause(Throwable cause) {
        var results = new ArrayList<Value>();
        for (var t = cause; t != null; t = t.getCause()) {
            results.add(Value.of(t.toString()));
        }
        return Value.of(new Array(results));
    }
}
