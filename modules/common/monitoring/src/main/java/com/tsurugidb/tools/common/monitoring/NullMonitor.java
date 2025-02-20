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

import java.util.List;

import com.tsurugidb.tools.common.diagnostic.DiagnosticCode;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.value.Property;

/**
 * An implementation of {@link Monitor} that does nothing.
 */
public enum NullMonitor implements Monitor {

    /**
     * The singleton instance of {@link NullMonitor}.
     */
    INSTANCE,
    ;

    @Override
    public void onStart() {
        // does nothing.
        return;
    }

    @Override
    public void onData(String format, List<? extends Property> properties) {
        // does nothing.
        return;
    }

    @Override
    public void onSuccess() {
        // does nothing.
        return;
    }

    @Override
    public void onFailure(Throwable cause, DiagnosticCode code, List<?> arguments) {
        // does nothing.
        return;
    }

    @Override
    public void onFailure(DiagnosticException exception) throws MonitoringException {
        // does nothing.
        return;
    }

    @Override
    public void close() {
        // does nothing.
        return;
    }
}
