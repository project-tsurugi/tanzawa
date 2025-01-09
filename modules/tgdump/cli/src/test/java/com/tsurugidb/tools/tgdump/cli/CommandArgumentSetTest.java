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
package com.tsurugidb.tools.tgdump.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.beust.jcommander.ParameterException;
import com.tsurugidb.tools.tgdump.core.engine.QueryDumpTargetSelector;
import com.tsurugidb.tools.tgdump.core.engine.TableDumpTargetSelector;

class CommandArgumentSetTest {

    @Test
    void getProfileReader() {
        var args = new CommandArgumentSet();
        assertNotNull(args.getProfileReader());
    }

    @Test
    void getProfileBundleLoader() {
        var args = new CommandArgumentSet();
        assertNotNull(args.getProfileBundleLoader());
    }

    @Test
    void getTargetSelector() {
        var args = new CommandArgumentSet();
        var selector = args.getTargetSelector();
        assertNotNull(selector);
        assertInstanceOf(TableDumpTargetSelector.class, selector);
    }

    @Test
    void getTargetSelector_queryMode() {
        var args = new CommandArgumentSet();
        args.setQueryMode(true);
        var selector = args.getTargetSelector();
        assertNotNull(selector);
        assertInstanceOf(QueryDumpTargetSelector.class, selector);
    }

    @Test
    void getConnectionProvider() {
        var args = new CommandArgumentSet();
        assertNotNull(args.getConnectionProvider());
    }

    @Test
    void validateCombination_ok() {
        var args = new CommandArgumentSet();
        args.setSingleMode(true);
        args.setQueryMode(false);
        args.setTableNames(List.of("tbl"));
        args.validateCombination();
    }

    @Test
    void validateCombination_invalid_table() {
        var args = new CommandArgumentSet();
        args.setSingleMode(true);
        args.setQueryMode(false);
        args.setTableNames(List.of("t1", "t2"));
        assertThrows(ParameterException.class, () -> args.validateCombination());
    }

    @Test
    void validateCombination_invalid_query() {
        var args = new CommandArgumentSet();
        args.setSingleMode(true);
        args.setQueryMode(true);
        args.setTableNames(List.of("t1", "t2"));
        assertThrows(ParameterException.class, () -> args.validateCombination());
    }
}
