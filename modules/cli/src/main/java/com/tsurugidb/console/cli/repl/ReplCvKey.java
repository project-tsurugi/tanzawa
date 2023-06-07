package com.tsurugidb.console.cli.repl;

import com.tsurugidb.console.core.config.ScriptCvKey;
import com.tsurugidb.console.core.config.ScriptCvKey.ScriptCvKeyColor;

/**
 * client variable key.
 */
public final class ReplCvKey {

    /** console.info.color */
    public static final ScriptCvKeyColor CONSOLE_INFO_COLOR = new ScriptCvKeyColor("console.info.color"); //$NON-NLS-1$
    /** console.implicit.color */
    public static final ScriptCvKeyColor CONSOLE_IMPLICIT_COLOR = new ScriptCvKeyColor("console.implicit.color"); //$NON-NLS-1$
    /** console.succeed.color */
    public static final ScriptCvKeyColor CONSOLE_SUCCEED_COLOR = new ScriptCvKeyColor("console.succeed.color"); //$NON-NLS-1$
    /** console.warning.color */
    public static final ScriptCvKeyColor CONSOLE_WARNING_COLOR = new ScriptCvKeyColor("console.warning.color"); //$NON-NLS-1$
    /** console.help.color */
    public static final ScriptCvKeyColor CONSOLE_HELP_COLOR = new ScriptCvKeyColor("console.help.color"); //$NON-NLS-1$

    /**
     * register key
     */
    public static void registerKey() {
        ScriptCvKey.registerKey(ReplCvKey.class);
    }

    private ReplCvKey() {
        return;
    }
}
