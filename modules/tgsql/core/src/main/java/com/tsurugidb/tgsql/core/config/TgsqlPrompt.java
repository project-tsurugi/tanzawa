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
package com.tsurugidb.tgsql.core.config;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.exception.TgsqlMessageException;
import com.tsurugidb.tgsql.core.executor.report.TransactionOptionReportUtil;
import com.tsurugidb.tgsql.core.executor.sql.TransactionWrapper;

/**
 * prompt.
 */
public class TgsqlPrompt {
    private static final Logger LOG = LoggerFactory.getLogger(TgsqlPrompt.class);

    /**
     * Creates a new instance.
     *
     * @param format prompt format
     * @return prompt
     */
    public static @Nullable TgsqlPrompt create(String format) {
        if (format.isEmpty()) {
            return null;
        }

        var pattern = Pattern.compile("(" + Pattern.quote("{{") + ")|(" + Pattern.quote("}}") + ")|\\{([^{}]*)\\}");
        var matcher = pattern.matcher(format);

        var elementList = new ArrayList<TgsqlPromptElement>();

        int index = 0;
        var sb = new StringBuilder();
        while (matcher.find()) {
            int end;
            end = appendSb(format, index, matcher, 1, sb, "{");
            if (end >= 0) {
                index = end;
                continue;
            }
            end = appendSb(format, index, matcher, 2, sb, "}");
            if (end >= 0) {
                index = end;
                continue;
            }
            index = addList(format, index, matcher, 3, sb, elementList);
        }
        addSbToList(format, index, format.length(), sb, elementList);

        if (elementList.isEmpty()) {
            elementList.add(TgsqlPromptElement.text(format));
        }

        return new TgsqlPrompt(format, elementList);
    }

    private static int appendSb(String format, int index, Matcher matcher, int group, StringBuilder sb, String text) {
        if (matcher.group(group) == null) {
            return -1;
        }

        int start = matcher.start(group);
        int end = matcher.end(group);
        if (index < start) {
            String s = format.substring(index, start);
            sb.append(s);
        }

        sb.append(text);

        return end;
    }

    private static int addList(String format, int index, Matcher matcher, int group, StringBuilder sb, List<TgsqlPromptElement> elementList) {
        int start = matcher.start(group) - 1;
        int end = matcher.end(group) + 1;
        addSbToList(format, index, start, sb, elementList);

        String text = matcher.group(group);
        var element = parseElement(text);
        elementList.add(element);

        return end;
    }

    private static void addSbToList(String format, int index, int start, StringBuilder sb, List<TgsqlPromptElement> elementList) {
        if (index < start) {
            String s = format.substring(index, start);
            sb.append(s);
        }

        if (sb.length() != 0) {
            String s = escape(sb);
            elementList.add(TgsqlPromptElement.text(s));
            sb.setLength(0);
        }
    }

    private static String escape(StringBuilder sb) {
        return sb.toString().replace("\\t", "\t").replace("\\r", "\r").replace("\\n", "\n");
    }

    private static TgsqlPromptElement parseElement(String text) {
        String target, originalField, field;
        {
            int n = text.indexOf('.');
            if (n >= 0) {
                target = text.substring(0, n).trim().toLowerCase(Locale.ENGLISH);
                originalField = text.substring(n + 1);
                field = originalField.trim().toLowerCase(Locale.ENGLISH).replaceAll("[_-]", "");
            } else {
                target = text;
                originalField = "";
                field = "";
            }
        }

        switch (target) {
        case "now":
            return parseElementDateTime(originalField);
        case "endpoint":
            return TgsqlPrompt::getEndpoint;
        case "connection":
        case "conn":
            switch (field) {
            case "uri":
                return TgsqlPrompt::getEndpoint;
            case "label":
                return TgsqlPrompt::getConnectionLabel;
            default:
                break;
            }
            break;
        case "transaction":
        case "tx":
            switch (field) {
            case "id":
                return TgsqlPrompt::getTransactionId;
            case "option":
                return TgsqlPrompt::getTxOption;
            case "type":
                return TgsqlPrompt::getTxType;
            case "label":
                return TgsqlPrompt::getTxLabel;
            case "includeddl":
            case "includedefinition":
            case "includedefinitions":
                return TgsqlPrompt::getTxIncludeDdl;
            case "wp":
            case "writepreserve":
                return TgsqlPrompt::getTxWritePreserve;
            case "ira":
            case "inclusivereadarea":
                return TgsqlPrompt::getTxInclusiveReadArea;
            case "era":
            case "exclusivereadarea":
                return TgsqlPrompt::getTxExclusiveReadArea;
            case "priority":
                return TgsqlPrompt::getTxPriority;
            default:
                break;
            }
            break;
        default:
            break;
        }

        LOG.trace("unsupported prompt.parseElement. text=[{}]", text);
        String s = "{" + target + "." + field + "}";
        return TgsqlPromptElement.text(s);
    }

    /**
     * prompt element.
     */
    @FunctionalInterface
    protected interface TgsqlPromptElement {

        /**
         * Creates a text prompt.
         *
         * @param text text
         * @return prompt element for text
         */
        static TgsqlPromptElement text(String text) {
            return (config, transaction) -> text;
        }

        /**
         * get prompt.
         *
         * @param config      tgsql configuration
         * @param transaction transaction
         * @return prompt text
         */
        String get(TgsqlConfig config, TransactionWrapper transaction);
    }

    static TgsqlPromptElement parseElementDateTime(String format) {
        if (format == null || format.isEmpty()) {
            return (c, t) -> LocalDateTime.now().toString();
        }

        try {
            var formatter = DateTimeFormatter.ofPattern(format);
            return (c, t) -> ZonedDateTime.now().format(formatter);
        } catch (Exception e) {
            throw new TgsqlMessageException(MessageFormat.format("dateTime format error. format={0}, cause={1}", format, e.getMessage()), e);
        }
    }

    static String getEndpoint(TgsqlConfig config, TransactionWrapper transaction) {
        return config.getEndpoint();
    }

    static String getConnectionLabel(TgsqlConfig config, TransactionWrapper transaction) {
        return config.getConnectionLabel().orElse(null);
    }

    static String getTransactionId(TgsqlConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var tx = transaction.getTransaction();
        return tx.getTransactionId();
    }

    static String getTxOption(TgsqlConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toString(option);
    }

    static String getTxType(TgsqlConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var type = option.getType();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toString(type);
    }

    static String getTxLabel(TgsqlConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        String label = option.getLabel();
        if (label == null) {
            return "";
        }
        return label;
    }

    static String getTxIncludeDdl(TgsqlConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        boolean includeDdl = option.getModifiesDefinitions();
        return Boolean.toString(includeDdl);
    }

    static String getTxWritePreserve(TgsqlConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var list = option.getWritePreservesList();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toStringWp(list);
    }

    static String getTxInclusiveReadArea(TgsqlConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var list = option.getInclusiveReadAreasList();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toStringRa(list);
    }

    static String getTxExclusiveReadArea(TgsqlConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var list = option.getExclusiveReadAreasList();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toStringRa(list);
    }

    static String getTxPriority(TgsqlConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var priority = option.getPriority();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toString(priority);
    }

    private final String format;
    private final List<TgsqlPromptElement> elementList;

    /**
     * Creates a new instance.
     *
     * @param format      prompt format
     * @param elementList list of prompt element
     */
    public TgsqlPrompt(String format, List<TgsqlPromptElement> elementList) {
        this.format = format;
        this.elementList = elementList;
    }

    /**
     * get prompt.
     *
     * @param config      tgsql configuration
     * @param transaction transaction
     * @return prompt
     */
    public String getPrompt(TgsqlConfig config, TransactionWrapper transaction) {
        if (elementList.size() == 1) {
            var element = elementList.get(0);
            return element.get(config, transaction);
        }

        var sb = new StringBuilder();
        for (var element : elementList) {
            String s;
            try {
                s = element.get(config, transaction);
            } catch (Exception e) {
                LOG.debug("PromptElement.get error", e);
                s = "<error>";
            }
            sb.append(s);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.format;
    }
}
