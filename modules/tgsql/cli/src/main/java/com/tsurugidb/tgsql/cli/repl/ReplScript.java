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
package com.tsurugidb.tgsql.cli.repl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.cli.repl.jline.ReplJLineParser.ParsedStatement;
import com.tsurugidb.tgsql.core.TgsqlRunner.StatementSupplier;
import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.config.TgsqlPrompt;
import com.tsurugidb.tgsql.core.config.TgsqlCvKey.TgsqlCvKeyPrompt;
import com.tsurugidb.tgsql.core.exception.TgsqlInterruptedException;
import com.tsurugidb.tgsql.core.executor.sql.TransactionWrapper;
import com.tsurugidb.tgsql.core.model.Region;
import com.tsurugidb.tgsql.core.model.SimpleStatement;
import com.tsurugidb.tgsql.core.model.Statement;
import com.tsurugidb.tgsql.core.model.Statement.Kind;

/**
 * Tsurugi SQL console repl script.
 */
public class ReplScript implements StatementSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(ReplScript.class);

    private static final String PROMPT1 = "tgsql> "; //$NON-NLS-1$
    private static final String PROMPT2 = "     | "; //$NON-NLS-1$

    private final LineReader lineReader;

    /**
     * Creates a new instance.
     *
     * @param lineReader LineReader
     */
    public ReplScript(@Nonnull LineReader lineReader) {
        this.lineReader = lineReader;
    }

    @Override
    public List<Statement> get(TgsqlConfig config, @Nullable TransactionWrapper transaction) {
        String prompt2 = getPrompt(config, ReplCvKey.PROMPT2_DEFAULT, ReplCvKey.PROMPT2_TRANSACTION, ReplCvKey.PROMPT2_OCC, ReplCvKey.PROMPT2_LTX, ReplCvKey.PROMPT2_RTX, PROMPT2, transaction);
        lineReader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, prompt2);

        String text;
        try {
            String prompt1 = getPrompt(config, ReplCvKey.PROMPT1_DEFAULT, ReplCvKey.PROMPT1_TRANSACTION, ReplCvKey.PROMPT1_OCC, ReplCvKey.PROMPT1_LTX, ReplCvKey.PROMPT1_RTX, PROMPT1, transaction);
            text = lineReader.readLine(prompt1);
        } catch (UserInterruptException e) {
            throw new TgsqlInterruptedException(e);
        } catch (EndOfFileException e) {
            LOG.trace("EndOfFileException", e); //$NON-NLS-1$
            return null;
        }

        var line = lineReader.getParsedLine();
        if (line instanceof ParsedStatement) {
            var statementList = ((ParsedStatement) line).statements();
            if (!statementList.isEmpty()) {
                return statementList;
            }
            var s = new SimpleStatement(Kind.EMPTY, text, new Region(0, text.length(), 0, 0));
            return List.of(s);
        }
        throw new AssertionError(line);
    }

    private String getPrompt(TgsqlConfig config, TgsqlCvKeyPrompt keyDefault, TgsqlCvKeyPrompt keyTx, TgsqlCvKeyPrompt keyOcc, TgsqlCvKeyPrompt keyLtx, TgsqlCvKeyPrompt keyRtx,
            String defaultPrompt, @Nullable TransactionWrapper transaction) {
        var variableMap = config.getClientVariableMap();

        TgsqlPrompt prompt = null;
        TgsqlCvKeyPrompt key = null;
        if (transaction != null) {
            switch (transaction.getOption().getType()) {
            case SHORT:
                key = keyOcc;
                break;
            case LONG:
                key = keyLtx;
                break;
            case READ_ONLY:
                key = keyRtx;
                break;
            default:
                break;
            }
            if (key != null) {
                prompt = variableMap.get(key);
            }
            if (prompt == null) {
                key = keyTx;
                prompt = variableMap.get(key);
            }
        }
        if (prompt == null) {
            key = keyDefault;
            prompt = variableMap.get(key);
            if (prompt == null) {
                return defaultPrompt;
            }
        }

        try {
            return prompt.getPrompt(config, transaction);
        } catch (Exception e) {
            LOG.debug("ReplScript.getPrompt error (key={}, prompt={})", key, prompt, e); //$NON-NLS-1$
            return defaultPrompt;
        }
    }
}
