package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.executor.engine.HelpMessage;
import com.tsurugidb.console.core.executor.report.ScriptReporter;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Help command for Tsurugi SQL console.
 */
public class HelpCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HelpCommand.class);

    private static final HelpMessage HELP_MESSAGE = initializeHelp();

    private static HelpMessage initializeHelp() {
        try {
            return HelpMessage.load(Locale.getDefault().getLanguage());
        } catch (IOException e) {
            LOG.warn("failed to load help message bundle", e);
            return new HelpMessage("help message bundle is not available.");
        }
    }

    /**
     * Creates a new instance.
     */
    public HelpCommand() {
        super("help", "h", "?"); //$NON-NLS-1$
    }

    @Override
    protected void collectCompleterCandidate(List<List<String>> result) {
        var list = createCompleterCandidateList();
        result.addAll(list);
    }

    private static List<List<String>> createCompleterCandidateList() {
        var result = new ArrayList<List<String>>();

        String helpCommand = "\\help"; //$NON-NLS-1$

        var keyList = HELP_MESSAGE.getKeys();
        for (String key : keyList) {
            boolean command = key.startsWith(HelpMessage.KEY_PREFIX_COMMAND);
            boolean special = key.startsWith(HelpMessage.KEY_PREFIX_SPECIAL_COMMAND);
            if (command || special) {
                List<String> keyWords = List.of(key.split(Pattern.quote("."))); //$NON-NLS-1$
                assert keyWords.size() >= 2;

                var candidate = new ArrayList<String>(keyWords.size());
                candidate.add(helpCommand);
                if (special) {
                    candidate.add(SpecialCommand.COMMAND_PREFIX + keyWords.get(1));
                    candidate.addAll(keyWords.subList(2, keyWords.size()));
                } else {
                    candidate.addAll(keyWords.subList(1, keyWords.size()));
                }

                result.add(candidate);
            }
        }

        if (result.isEmpty()) {
            result.add(List.of(helpCommand));
        }

        return result;
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        LOG.debug("show help"); //$NON-NLS-1$
        List<String> message = HELP_MESSAGE.find(statement);
        var reporter = engine.getReporter();
        reporter.reportHelp(message);
        return true;
    }

    /**
     * Print help message.
     * 
     * @param commandName target command name
     * @param reporter    reporter
     */
    public static void printHelp(String commandName, ScriptReporter reporter) {
        List<String> message = HELP_MESSAGE.findForSpecialCommand(commandName);
        reporter.reportHelp(message);
    }
}
