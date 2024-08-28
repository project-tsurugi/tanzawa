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
package com.tsurugidb.tgsql.core.executor.result;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.tsurugidb.tgsql.core.executor.IoSupplier;

/**
 * Provides standard output.
 */
public class StandardWriterSupplier implements IoSupplier<Writer> {

    private final Charset charset;

    /**
     * Creates a new instance with the default charset.
     */
    public StandardWriterSupplier() {
        this(Charset.defaultCharset());
    }

    /**
     * Creates a new instance.
     * 
     * @param charset the output charset
     */
    public StandardWriterSupplier(@Nonnull Charset charset) {
        Objects.requireNonNull(charset);
        this.charset = charset;
    }

    @Override
    public Writer get() throws IOException {
        var out = new FilterOutputStream(System.out) {
            @Override
            public void close() throws IOException {
                flush();
                // super.close(); // System.out does not close
            }
        };
        return new OutputStreamWriter(out, charset);
    }
}
