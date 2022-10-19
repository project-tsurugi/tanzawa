package com.tsurugidb.console.cli.repl.jline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.function.Function;

import org.jline.reader.LineReader;
import org.jline.reader.impl.history.DefaultHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tsurugi SQL console JLine History.
 */
public class ReplJLineHistory extends DefaultHistory {
    private static final Logger LOG = LoggerFactory.getLogger(ReplJLineTerminal.class);

    private LineReader lineReader;

    @Override
    public void attach(LineReader reader) {
        this.lineReader = reader;
        super.attach(reader);
    }

    @Override
    public void load() throws IOException {
        try {
            var path = getPath();
            if (path != null && Files.exists(path)) {
                LOG.trace("Loading history from: {}", path);
                if (lineReader.isSet(LineReader.Option.HISTORY_TIMESTAMPED)) {
                    load(path, line -> line.substring(line.indexOf(':') + 1));
                } else {
                    load(path, line -> line);
                }
            }
        } catch (Exception e) {
            LOG.debug("Failed to load history", e);
            throw e;
        }
    }

    private Path getPath() {
        Object obj = lineReader.getVariable(LineReader.HISTORY_FILE);
        return (Path) obj;
    }

    private void load(Path path, Function<String, String> keyExtractor) throws IOException {
        var map = new LinkedHashMap<String, String>(64, 0.75f, true);
        try (var reader = Files.newBufferedReader(path)) {
            reader.lines().forEach(line -> {
                var key = keyExtractor.apply(line).trim();
                map.put(key, line);
            });
        }
        for (var line : map.values()) {
            addHistoryLine(path, line);
        }
    }
}
