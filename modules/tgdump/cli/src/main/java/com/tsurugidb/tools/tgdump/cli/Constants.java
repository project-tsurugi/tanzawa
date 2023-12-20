package com.tsurugidb.tools.tgdump.cli;

/**
 * The application constants of Tsurugi Dump Tool.
 */
public final class Constants {

    /**
     * The application name.
     */
    public static final String APPLICATION_NAME = "tgdump";

    /**
     * The exit status value of successful.
     */
    public static final int EXIT_STATUS_OK = 0;

    /**
     * The exit status value of operation errors.
     */
    public static final int EXIT_STATUS_OPERATION_ERROR = 1;

    /**
     * The exit status value of parameter errors.
     */
    public static final int EXIT_STATUS_PARAMETER_ERROR = 2;

    /**
     * The exit status value of monitoring errors.
     */
    public static final int EXIT_STATUS_MONITOR_ERROR = 3;

    /**
     * The exit status value of internal errors.
     */
    public static final int EXIT_STATUS_INTERNAL_ERROR = 4;

    /**
     * The exit status value of operation interrupted.
     */
    public static final int EXIT_STATUS_INTERRUPTED = 5;

    private Constants() {
        return;
    }
}
