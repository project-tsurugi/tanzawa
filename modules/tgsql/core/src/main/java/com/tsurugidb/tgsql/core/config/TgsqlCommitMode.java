package com.tsurugidb.tgsql.core.config;

/**
 * Commit mode.
 */
public enum TgsqlCommitMode {
    /** commit every statement. */
    AUTO_COMMIT,
    /** commit only if user explicitly specify a COMMIT statement. */
    NO_AUTO_COMMIT,

    /** commit on success, rollback on failure. */
    COMMIT,
    /** always rollback. */
    NO_COMMIT,
}
