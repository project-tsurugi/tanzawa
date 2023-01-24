package com.tsurugidb.console.core.executor.engine.command;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.config.ScriptCvKey;
import com.tsurugidb.console.core.executor.engine.BasicEngine;
import com.tsurugidb.console.core.executor.engine.EngineException;
import com.tsurugidb.console.core.model.SpecialStatement;
import com.tsurugidb.tsubakuro.exception.ServerException;

/**
 * Timing command for Tsurugi SQL console.
 */
public class TimingCommand extends SpecialCommand {
    private static final Logger LOG = LoggerFactory.getLogger(TimingCommand.class);

    private static final String COMMAND_NAME = "timing"; //$NON-NLS-1$
    private static final String COMMAND = COMMAND_PREFIX + COMMAND_NAME;

    /**
     * Creates a new instance.
     */
    public TimingCommand() {
        super(COMMAND_NAME);
    }

    @Override
    protected void collectCompleterCandidate(List<CompleterCandidateWords> result) {
        result.add(new CompleterCandidateWords(COMMAND, true));
        result.add(new CompleterCandidateWords(COMMAND, "on", true));
        result.add(new CompleterCandidateWords(COMMAND, "off", true));
    }

    @Override
    public boolean execute(BasicEngine engine, SpecialStatement statement) throws EngineException, ServerException, IOException, InterruptedException {
        var clientVariableMap = engine.getConfig().getClientVariableMap();
        var option = getOption(statement, 0);

        boolean value;
        if (option == null) {
            LOG.debug("timing"); //$NON-NLS-1$
            value = !clientVariableMap.get(ScriptCvKey.TIMING, false);
        } else {
            LOG.debug("timing {}", option); //$NON-NLS-1$
            value = ScriptCvKey.TIMING.convertValue(option);
        }

        clientVariableMap.put(ScriptCvKey.TIMING, value);

        var message = MessageFormat.format("Timing is {0}.", value ? "on" : "off"); //$NON-NLS-1$
        var reporter = engine.getReporter();
        reporter.info(message);

        return true;
    }
}
