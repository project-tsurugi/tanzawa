package com.tsurugidb.console.core.config;

/**
 * Commit mode.
 */
public enum ScriptCommitMode {
    /** commit every statement */
    AUTO_COMMIT,
    /** commit only if user explicitly specify a COMMIT statement */
    NO_AUTO_COMMIT,

    /** commit on success, rollback on failure */
    COMMIT,
    /** always rollback */
    NO_COMMIT,
}
