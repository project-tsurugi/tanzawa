package com.tsurugidb.console.cli.repl.jline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.impl.history.DefaultHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.console.core.executor.engine.command.HistoryCommand;
import com.tsurugidb.console.core.executor.report.HistoryEntry;

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

    /**
     * create history supplier.
     *
     * @param history JLine History
     * @return history supplier
     * @see HistoryCommand
     */
    public static IntFunction<Iterator<HistoryEntry>> createHistorySupplier(History history) {
        return size -> {
            int start;
            {
                int first = history.first();
                if (size < 0) {
                    start = first;
                } else {
                    int max = history.last() + 1;
                    int index = max - size;
                    start = Math.max(first, index);
                }
            }

            var iterator = history.iterator(start);
            return new Iterator<HistoryEntry>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public HistoryEntry next() {
                    var entry = iterator.next();
                    return new HistoryEntry(entry.index(), entry.line());
                }
            };
        };
    }
}
