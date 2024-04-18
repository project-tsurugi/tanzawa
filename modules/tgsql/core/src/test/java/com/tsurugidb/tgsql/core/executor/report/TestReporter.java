package com.tsurugidb.tgsql.core.executor.report;

import com.tsurugidb.tgsql.core.config.TgsqlConfig;

public class TestReporter extends BasicReporter {

    public TestReporter() {
        this(new TgsqlConfig());
    }

    public TestReporter(TgsqlConfig config) {
        super(config);
    }
}
