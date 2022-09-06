package com.tsurugidb.console.cli.jline;

import java.net.URI;

import com.tsurugidb.console.core.ScriptRunner;
import com.tsurugidb.console.core.executor.BasicEngine;
import com.tsurugidb.console.core.executor.BasicSqlProcessor;
import com.tsurugidb.tsubakuro.channel.common.connection.Credential;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.common.SessionBuilder;
import com.tsurugidb.tsubakuro.sql.SqlClient;

public final class JLineMain {

    public static void main(String... args) throws Exception {
        var endpoint = URI.create(args[1]);
        Credential credential = NullCredential.INSTANCE;

        var lineReader = JlLineReader.create();

        try (var reader = new JlIoReader(lineReader);
                var session = SessionBuilder.connect(endpoint).withCredential(credential).create();
                var sqlProcessor = new BasicSqlProcessor(SqlClient.attach(session));
                var resultProcessor = new JlResultProcessor(lineReader.getTerminal())) {
            ScriptRunner.repl(reader, new BasicEngine(sqlProcessor, resultProcessor));
        }
    }

    private JLineMain() {
        throw new AssertionError();
    }
}
