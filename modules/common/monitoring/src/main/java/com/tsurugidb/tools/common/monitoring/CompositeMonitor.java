/*
 * Copyright 2023-2025 Project Tsurugi.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.value.Property;

/**
 * A {@link Monitor} that dispatches each message to sub-monitors.
 */
public class CompositeMonitor implements Monitor {

    private final Monitor[] elements;

    /**
     * Creates a new instance.
     * @param elements the element monitors
     */
    public CompositeMonitor(@Nonnull Monitor... elements) {
        Objects.requireNonNull(elements);
        this.elements = Arrays.copyOf(elements, elements.length);
    }

    /**
     * Creates a new instance.
     * @param elements the element monitors
     */
    public CompositeMonitor(@Nonnull Collection<? extends Monitor> elements) {
        Objects.requireNonNull(elements);
        this.elements = elements.toArray(new Monitor[elements.size()]);
    }

    @Override
    public void onStart() throws MonitoringException {
        for (Monitor element : elements) {
            if (element != null) {
                element.onStart();
            }
        }
    }

    @Override
    public void onData(@Nonnull String format, @Nonnull List<? extends Property> properties) throws MonitoringException {
        Objects.requireNonNull(format);
        Objects.requireNonNull(properties);
        for (Monitor element : elements) {
            if (element != null) {
                element.onData(format, properties);
            }
        }
    }

    @Override
    public void onSuccess() throws MonitoringException {
        for (Monitor element : elements) {
            if (element != null) {
                element.onSuccess();
            }
        }
    }

    @Override
    public void onFailure(@Nullable Throwable cause, @Nonnull DiagnosticCode code, @Nonnull List<?> arguments)
            throws MonitoringException {
        Objects.requireNonNull(code);
        Objects.requireNonNull(arguments);
        for (Monitor element : elements) {
            if (element != null) {
                element.onFailure(cause, code, arguments);
            }
        }
    }

    @Override
    public void onFailure(@Nonnull DiagnosticException exception) throws MonitoringException {
        Objects.requireNonNull(exception);
        for (Monitor element : elements) {
            if (element != null) {
                element.onFailure(exception);
            }
        }
    }

    @Override
    public void close() throws IOException {
        IOException occurred = null;
        for (int i = 0; i < elements.length; i++) {
            var element = elements[i];
            if (element == null) {
                continue;
            }
            try {
                element.close();
                elements[i] = null;
            } catch (IOException e) {
                if (occurred == null) {
                    occurred = e;
                } else {
                    occurred.addSuppressed(e);
                }
            }
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(elements);
    }
}
