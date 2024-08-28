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
package com.tsurugidb.tgsql.cli.repl;

import com.tsurugidb.tgsql.core.config.TgsqlCvKey;
import com.tsurugidb.tgsql.core.config.TgsqlCvKey.TgsqlCvKeyColor;
import com.tsurugidb.tgsql.core.config.TgsqlCvKey.TgsqlCvKeyPrompt;

/**
 * client variable key.
 */
public final class ReplCvKey {

    /** console.prompt1.default . */
    public static final TgsqlCvKeyPrompt PROMPT1_DEFAULT = new TgsqlCvKeyPrompt("console.prompt1.default"); //$NON-NLS-1$
    /** console.prompt1.tx . */
    public static final TgsqlCvKeyPrompt PROMPT1_TRANSACTION = new TgsqlCvKeyPrompt("console.prompt1.tx"); //$NON-NLS-1$
    /** console.prompt1.occ . */
    public static final TgsqlCvKeyPrompt PROMPT1_OCC = new TgsqlCvKeyPrompt("console.prompt1.occ"); //$NON-NLS-1$
    /** console.prompt1.ltx . */
    public static final TgsqlCvKeyPrompt PROMPT1_LTX = new TgsqlCvKeyPrompt("console.prompt1.ltx"); //$NON-NLS-1$
    /** console.prompt1.rtx . */
    public static final TgsqlCvKeyPrompt PROMPT1_RTX = new TgsqlCvKeyPrompt("console.prompt1.rtx"); //$NON-NLS-1$
    /** console.prompt2.default . */
    public static final TgsqlCvKeyPrompt PROMPT2_DEFAULT = new TgsqlCvKeyPrompt("console.prompt2.default"); //$NON-NLS-1$
    /** console.prompt2.tx . */
    public static final TgsqlCvKeyPrompt PROMPT2_TRANSACTION = new TgsqlCvKeyPrompt("console.prompt2.tx"); //$NON-NLS-1$
    /** console.prompt2.occ . */
    public static final TgsqlCvKeyPrompt PROMPT2_OCC = new TgsqlCvKeyPrompt("console.prompt2.occ"); //$NON-NLS-1$
    /** console.prompt2.ltx . */
    public static final TgsqlCvKeyPrompt PROMPT2_LTX = new TgsqlCvKeyPrompt("console.prompt2.ltx"); //$NON-NLS-1$
    /** console.prompt2.rtx . */
    public static final TgsqlCvKeyPrompt PROMPT2_RTX = new TgsqlCvKeyPrompt("console.prompt2.rtx"); //$NON-NLS-1$

    /** console.info.color . */
    public static final TgsqlCvKeyColor CONSOLE_INFO_COLOR = new TgsqlCvKeyColor("console.info.color"); //$NON-NLS-1$
    /** console.implicit.color . */
    public static final TgsqlCvKeyColor CONSOLE_IMPLICIT_COLOR = new TgsqlCvKeyColor("console.implicit.color"); //$NON-NLS-1$
    /** console.succeed.color . */
    public static final TgsqlCvKeyColor CONSOLE_SUCCEED_COLOR = new TgsqlCvKeyColor("console.succeed.color"); //$NON-NLS-1$
    /** console.warning.color . */
    public static final TgsqlCvKeyColor CONSOLE_WARNING_COLOR = new TgsqlCvKeyColor("console.warning.color"); //$NON-NLS-1$
    /** console.help.color . */
    public static final TgsqlCvKeyColor CONSOLE_HELP_COLOR = new TgsqlCvKeyColor("console.help.color"); //$NON-NLS-1$

    /**
     * register key.
     */
    public static void registerKey() {
        TgsqlCvKey.registerKey(ReplCvKey.class);
    }

    private ReplCvKey() {
        return;
    }
}
