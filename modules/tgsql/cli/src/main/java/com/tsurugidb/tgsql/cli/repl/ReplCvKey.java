package com.tsurugidb.tgsql.cli.repl;

import com.tsurugidb.tgsql.core.config.ScriptCvKey;
import com.tsurugidb.tgsql.core.config.ScriptCvKey.ScriptCvKeyColor;
import com.tsurugidb.tgsql.core.config.ScriptCvKey.ScriptCvKeyPrompt;

/**
 * client variable key.
 */
public final class ReplCvKey {

    /** console.prompt1.default . */
    public static final ScriptCvKeyPrompt PROMPT1_DEFAULT = new ScriptCvKeyPrompt("console.prompt1.default"); //$NON-NLS-1$
    /** console.prompt1.tx . */
    public static final ScriptCvKeyPrompt PROMPT1_TRANSACTION = new ScriptCvKeyPrompt("console.prompt1.tx"); //$NON-NLS-1$
    /** console.prompt1.occ . */
    public static final ScriptCvKeyPrompt PROMPT1_OCC = new ScriptCvKeyPrompt("console.prompt1.occ"); //$NON-NLS-1$
    /** console.prompt1.ltx . */
    public static final ScriptCvKeyPrompt PROMPT1_LTX = new ScriptCvKeyPrompt("console.prompt1.ltx"); //$NON-NLS-1$
    /** console.prompt1.rtx . */
    public static final ScriptCvKeyPrompt PROMPT1_RTX = new ScriptCvKeyPrompt("console.prompt1.rtx"); //$NON-NLS-1$
    /** console.prompt2.default . */
    public static final ScriptCvKeyPrompt PROMPT2_DEFAULT = new ScriptCvKeyPrompt("console.prompt2.default"); //$NON-NLS-1$
    /** console.prompt2.tx . */
    public static final ScriptCvKeyPrompt PROMPT2_TRANSACTION = new ScriptCvKeyPrompt("console.prompt2.tx"); //$NON-NLS-1$
    /** console.prompt2.occ . */
    public static final ScriptCvKeyPrompt PROMPT2_OCC = new ScriptCvKeyPrompt("console.prompt2.occ"); //$NON-NLS-1$
    /** console.prompt2.ltx . */
    public static final ScriptCvKeyPrompt PROMPT2_LTX = new ScriptCvKeyPrompt("console.prompt2.ltx"); //$NON-NLS-1$
    /** console.prompt2.rtx . */
    public static final ScriptCvKeyPrompt PROMPT2_RTX = new ScriptCvKeyPrompt("console.prompt2.rtx"); //$NON-NLS-1$

    /** console.info.color . */
    public static final ScriptCvKeyColor CONSOLE_INFO_COLOR = new ScriptCvKeyColor("console.info.color"); //$NON-NLS-1$
    /** console.implicit.color . */
    public static final ScriptCvKeyColor CONSOLE_IMPLICIT_COLOR = new ScriptCvKeyColor("console.implicit.color"); //$NON-NLS-1$
    /** console.succeed.color . */
    public static final ScriptCvKeyColor CONSOLE_SUCCEED_COLOR = new ScriptCvKeyColor("console.succeed.color"); //$NON-NLS-1$
    /** console.warning.color . */
    public static final ScriptCvKeyColor CONSOLE_WARNING_COLOR = new ScriptCvKeyColor("console.warning.color"); //$NON-NLS-1$
    /** console.help.color . */
    public static final ScriptCvKeyColor CONSOLE_HELP_COLOR = new ScriptCvKeyColor("console.help.color"); //$NON-NLS-1$

    /**
     * register key.
     */
    public static void registerKey() {
        ScriptCvKey.registerKey(ReplCvKey.class);
    }

    private ReplCvKey() {
        return;
    }
}
