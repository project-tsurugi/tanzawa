package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.executor.engine.HelpMessage;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Help command for Tsurugi SQL console.
 */
public class HelpCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(HelpCommand.class);

    private final HelpMessage help;

    /**
     * Creates a new instance.
     */
    public HelpCommand() {
        super("help", "h"); //$NON-NLS-1$
        this.help = initializeHelp();
    }

    private static HelpMessage initializeHelp() {
        try {
            return HelpMessage.load(Locale.getDefault().getLanguage());
        } catch (IOException e) {
            LOG.warn("failed to load help message bundle", e);
            return new HelpMessage("help message bundle is not available.");
        }
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        LOG.debug("show help"); //$NON-NLS-1$
        List<String> message = help.find(statement);
        var reporter = engine.getReporter();
        reporter.reportHelp(message);
        return true;
    }
}
