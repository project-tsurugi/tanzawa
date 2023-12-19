package com.tsurugidb.console.cli.repl;

import javax.annotation.Nonnull;

import com.tsurugidb.console.cli.repl.jline.ReplJLineReader;
import com.tsurugidb.console.core.credential.DefaultCredentialSessionConnector;

public class ReplDefaultCredentialSessionConnector extends DefaultCredentialSessionConnector {

    @Override
    public String readUser() {
        return readReplUser();
    }

    @Override
    public String readPassword() {
        return readReplPassword();
    }

    /**
     * get user from console.
     *
     * @return user
     */
    public static @Nonnull String readReplUser() {
        var lineReader = ReplJLineReader.createSimpleReader();
        return lineReader.readLine("user: ");
    }

    /**
     * get password from console.
     *
     * @return password
     */
    public static @Nonnull String readReplPassword() {
        var lineReader = ReplJLineReader.createSimpleReader();
        return lineReader.readLine("password: ", '*');
    }
}
