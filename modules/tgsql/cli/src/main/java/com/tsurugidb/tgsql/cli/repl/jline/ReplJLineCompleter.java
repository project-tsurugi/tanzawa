/*
 * Copyright 2023-2025 Project Tsurugi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tsurugidb.tgsql.cli.repl.jline;

import java.util.ArrayList;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.tsurugidb.tgsql.core.config.TgsqlConfig;
import com.tsurugidb.tgsql.core.executor.engine.command.SpecialCommand;

/**
 * Tsurugi SQL console JLine Completer.
 */
public class ReplJLineCompleter implements Completer {

    private final TgsqlConfig config;

    /**
     * Creates a new instance.
     *
     * @param config tgsql configuration
     */
    public ReplJLineCompleter(TgsqlConfig config) {
        this.config = config;
    }

    @Override
    public void complete(LineReader reader, ParsedLine commandLine, List<Candidate> candidates) {
        String line = commandLine.line().substring(0, commandLine.cursor());
        if (line.startsWith(SpecialCommand.COMMAND_PREFIX)) {
            completeSpecialCommand(line, candidates);
        } else {
            completeSqlCommand(line, candidates);
        }
    }

    private static List<SimpleCompleterCandidate> specialCommandCandidateList = null;

    private void completeSpecialCommand(String line, List<Candidate> candidates) {
        if (specialCommandCandidateList == null) {
            var list = SpecialCommand.getCompleterCandidateList();
            var temp = new ArrayList<SimpleCompleterCandidate>(list.size());
            for (var candidate : list) {
                temp.add(new SimpleCompleterCandidate(candidate.getWords(), candidate.getEnd()));
            }
            specialCommandCandidateList = temp;
        }
        collectCandidate(line, specialCommandCandidateList, candidates);
    }

    private static List<SimpleCompleterCandidate> sqCandidateList = null;

    private void completeSqlCommand(String line, List<Candidate> candidates) {
        // FIXME use SQL parser

        if (sqCandidateList == null) {
            sqCandidateList = getSqCandidateList();
        }
        collectCandidate(line, sqCandidateList, candidates);
    }

    private static List<SimpleCompleterCandidate> getSqCandidateList() {
        var result = new ArrayList<SimpleCompleterCandidate>();

        addTx(result, "start transaction");
        addTx(result, "start long transaction");
        addTx(result, "begin");
        addTx(result, "begin transaction");
        addTx(result, "begin long transaction");
        add(result, "commit;");
        add(result, "commit wait for accepted;");
        add(result, "commit wait for available;");
        add(result, "commit wait for stored;");
        add(result, "commit wait for propagated;");
        add(result, "rollback;");

        add(result, "call");

        add(result, "create table");
        add(result, "drop table");
        addDml(result, "");
        addDml(result, "explain ");

        return result;
    }

    private static void addTx(List<SimpleCompleterCandidate> result, String base) {
        String[] optionList = { //
                "read only immediate", //
                "read only deferrable", //
                "read write", //
                "write preserve", //
                "include definition", //
                "include definitions", //
                "include ddl", //
                "read area include", //
                "read area exclude", //
                "as", //
                "with", //
        };
        for (String option : optionList) {
            add(result, base + " " + option);
        }

        for (String option1 : List.of("prior", "excluding")) {
            for (String option2 : List.of("deferrable", "immediate")) {
                add(result, base + " execute " + option1 + " " + option2);
            }
        }
    }

    private static void addDml(List<SimpleCompleterCandidate> result, String base) {
        add(result, base + "select");
        add(result, base + "insert into");
        add(result, base + "update");
        add(result, base + "delete from");
    }

    private static void add(List<SimpleCompleterCandidate> result, String candidateLine) {
        String words = candidateLine.trim();
        boolean end = candidateLine.endsWith(";");
        var candidate = new SimpleCompleterCandidate(words, end);
        result.add(candidate);
    }

    private void collectCandidate(String line, List<SimpleCompleterCandidate> candidateList, List<Candidate> result) {
        var inputWords = SimpleCompleterCandidate.toWordList(line);
        for (var candidate : candidateList) {
            collectCandidate(inputWords, candidate, result);
        }

        if (inputWords.length >= 2) {
            String word0 = inputWords[0];
            if (word0.startsWith(SpecialCommand.COMMAND_PREFIX)) {
                var commandList = SpecialCommand.findCommand(word0.substring(1));
                if (commandList.size() == 1) {
                    var command = commandList.get(0).command();
                    var list = command.getDynamicCompleterCandidateList(config, inputWords);
                    for (var c : list) {
                        var candidate = new SimpleCompleterCandidate(c.getWords(), c.getEnd());
                        collectCandidate(inputWords, candidate, result);
                    }
                }
            }
        }
    }

    private void collectCandidate(String[] inputWords, SimpleCompleterCandidate candidate, List<Candidate> result) {
        var found = candidate.findCandidateWord(inputWords);
        if (found != null) {
            String word = found.word();
            boolean end = found.end();
            result.add(new Candidate(word, word, null, null, null, null, !end));
        }
    }
}
