package com.tsurugidb.tgsql.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.executor.report.TransactionOptionReportUtil;
import com.tsurugidb.tgsql.core.executor.sql.TransactionWrapper;

/**
 * prompt.
 */
public class ScriptPrompt {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptPrompt.class);

    /**
     * Creates a new instance.
     *
     * @param format prompt format
     * @return prompt
     */
    public static @Nullable ScriptPrompt create(String format) {
        if (format.isEmpty()) {
            return null;
        }

        var pattern = Pattern.compile("(" + Pattern.quote("{{") + ")|(" + Pattern.quote("}}") + ")|\\{([^{}]*)\\}");
        var matcher = pattern.matcher(format);

        var elementList = new ArrayList<ScriptPromptElement>();

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
            elementList.add(ScriptPromptElement.text(format));
        }

        return new ScriptPrompt(format, elementList);
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

    private static int addList(String format, int index, Matcher matcher, int group, StringBuilder sb, List<ScriptPromptElement> elementList) {
        int start = matcher.start(group) - 1;
        int end = matcher.end(group) + 1;
        addSbToList(format, index, start, sb, elementList);

        String text = matcher.group(group);
        var element = parseElement(text);
        elementList.add(element);

        return end;
    }

    private static void addSbToList(String format, int index, int start, StringBuilder sb, List<ScriptPromptElement> elementList) {
        if (index < start) {
            String s = format.substring(index, start);
            sb.append(s);
        }

        if (sb.length() != 0) {
            String s = escape(sb);
            elementList.add(ScriptPromptElement.text(s));
            sb.setLength(0);
        }
    }

    private static String escape(StringBuilder sb) {
        return sb.toString().replace("\\t", "\t").replace("\\r", "\r").replace("\\n", "\n");
    }

    private static ScriptPromptElement parseElement(String text) {
        String target, field;
        {
            int n = text.indexOf('.');
            if (n >= 0) {
                target = text.substring(0, n).trim().toLowerCase(Locale.ENGLISH);
                field = text.substring(n + 1).trim().toLowerCase(Locale.ENGLISH).replaceAll("[_-]", "");
            } else {
                target = text;
                field = "";
            }
        }

        switch (target) {
        case "endpoint":
            return ScriptPrompt::getEndpoint;
        case "tx":
            switch (field) {
            case "id":
                return ScriptPrompt::getTransactionId;
            case "option":
                return ScriptPrompt::getTxOption;
            case "type":
                return ScriptPrompt::getTxType;
            case "label":
                return ScriptPrompt::getTxLabel;
            case "includeddl":
            case "includedefinition":
            case "includedefinitions":
                return ScriptPrompt::getTxIncludeDdl;
            case "wp":
            case "writepreserve":
                return ScriptPrompt::getTxWritePreserve;
            case "ira":
            case "inclusivereadarea":
                return ScriptPrompt::getTxInclusiveReadArea;
            case "era":
            case "exclusivereadarea":
                return ScriptPrompt::getTxExclusiveReadArea;
            case "priority":
                return ScriptPrompt::getTxPriority;
            default:
                break;
            }
            break;
        default:
            break;
        }

        LOG.trace("unsupported prompt.parseElement. text=[{}]", text);
        String s = "{" + target + "." + field + "}";
        return ScriptPromptElement.text(s);
    }

    /**
     * prompt element.
     */
    @FunctionalInterface
    protected interface ScriptPromptElement {

        /**
         * Creates a text prompt.
         *
         * @param text text
         * @return prompt element for text
         */
        static ScriptPromptElement text(String text) {
            return (config, transaction) -> text;
        }

        /**
         * get prompt.
         *
         * @param config      script configuration
         * @param transaction transaction
         * @return prompt text
         */
        String get(ScriptConfig config, TransactionWrapper transaction);
    }

    static String getEndpoint(ScriptConfig config, TransactionWrapper transaction) {
        return config.getEndpoint();
    }

    static String getTransactionId(ScriptConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var tx = transaction.getTransaction();
        return tx.getTransactionId();
    }

    static String getTxOption(ScriptConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toString(option);
    }

    static String getTxType(ScriptConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var type = option.getType();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toString(type);
    }

    static String getTxLabel(ScriptConfig config, TransactionWrapper transaction) {
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

    static String getTxIncludeDdl(ScriptConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        boolean includeDdl = option.getModifiesDefinitions();
        return Boolean.toString(includeDdl);
    }

    static String getTxWritePreserve(ScriptConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var list = option.getWritePreservesList();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toStringWp(list);
    }

    static String getTxInclusiveReadArea(ScriptConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var list = option.getInclusiveReadAreasList();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toStringRa(list);
    }

    static String getTxExclusiveReadArea(ScriptConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var list = option.getExclusiveReadAreasList();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toStringRa(list);
    }

    static String getTxPriority(ScriptConfig config, TransactionWrapper transaction) {
        if (transaction == null) {
            return "<error>";
        }

        var option = transaction.getOption();
        var priority = option.getPriority();
        var util = TransactionOptionReportUtil.getInstance();
        return util.toString(priority);
    }

    private final String format;
    private final List<ScriptPromptElement> elementList;

    /**
     * Creates a new instance.
     *
     * @param format      prompt format
     * @param elementList list of prompt element
     */
    public ScriptPrompt(String format, List<ScriptPromptElement> elementList) {
        this.format = format;
        this.elementList = elementList;
    }

    /**
     * get prompt.
     *
     * @param config      script configuration
     * @param transaction transaction
     * @return prompt
     */
    public String getPrompt(ScriptConfig config, TransactionWrapper transaction) {
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
