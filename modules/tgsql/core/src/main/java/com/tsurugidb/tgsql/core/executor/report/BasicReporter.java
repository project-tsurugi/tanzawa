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
package com.tsurugidb.tgsql.core.executor.report;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.config.TgsqlConfig;

/**
 * A basic implementation of {@link TgsqlReporter}.
 */
public class BasicReporter extends TgsqlReporter {
    private static final Logger LOG = LoggerFactory.getLogger(BasicReporter.class);

    /**
     * Creates a new instance.
     *
     * @param config tgsql configuration
     */
    public BasicReporter(@Nonnull TgsqlConfig config) {
        super(config);
    }

    @Override
    protected void doInfo(String message) {
        LOG.info(message);
    }

    @Override
    protected void doImplicit(String message) {
        LOG.debug(message);
    }

    @Override
    protected void doSucceed(String message) {
        LOG.info(message);
    }

    @Override
    protected void doWarn(String message) {
        LOG.warn(message);
    }
}
