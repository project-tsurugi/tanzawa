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
