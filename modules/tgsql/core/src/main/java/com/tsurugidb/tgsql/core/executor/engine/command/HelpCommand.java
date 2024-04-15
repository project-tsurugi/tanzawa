package com.tsurugidb.tgsql.core.executor.engine.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.executor.engine.BasicEngine;
import com.tsurugidb.tgsql.core.executor.engine.EngineException;
import com.tsurugidb.tgsql.core.executor.engine.HelpMessage;
import com.tsurugidb.tgsql.core.executor.report.TgsqlReporter;
import com.tsurugidb.tgsql.core.model.SpecialStatement;
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
        super("help", "h", "?"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    protected void collectCompleterCandidate(List<CompleterCandidateWords> result) {
        var list = createCompleterCandidateList();
        result.addAll(list);
    }

    private static List<CompleterCandidateWords> createCompleterCandidateList() {
        var result = new ArrayList<CompleterCandidateWords>();

        String helpCommand = "\\help"; //$NON-NLS-1$

        var keyList = HELP_MESSAGE.getKeys();
        for (String key : keyList) {
            boolean command = key.startsWith(HelpMessage.KEY_PREFIX_COMMAND);
            boolean special = key.startsWith(HelpMessage.KEY_PREFIX_SPECIAL_COMMAND);
            if (command || special) {
                List<String> keyWords = List.of(key.split(Pattern.quote("."))); //$NON-NLS-1$
                assert keyWords.size() >= 2;

                var candidate = new CompleterCandidateWords(true);
                candidate.add(helpCommand);
                if (special) {
                    candidate.add(SpecialCommand.COMMAND_PREFIX + keyWords.get(1));
                    candidate.addAll(keyWords, 2);
                } else {
                    candidate.addAll(keyWords, 1);
                }

                result.add(candidate);
            }
        }

        if (result.isEmpty()) {
            result.add(new CompleterCandidateWords(helpCommand, true));
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
    public static void printHelp(String commandName, TgsqlReporter reporter) {
        List<String> message = HELP_MESSAGE.findForSpecialCommand(commandName);
        reporter.reportHelp(message);
    }
}
