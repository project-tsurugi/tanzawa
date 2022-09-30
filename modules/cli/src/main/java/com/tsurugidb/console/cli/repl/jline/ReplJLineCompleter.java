package com.tsurugidb.console.cli.repl.jline;

import java.util.ArrayList;
import java.util.List;

import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import com.tsurugidb.console.core.executor.engine.command.SpecialCommand;

/**
 * Tsurugi SQL console JLine Completer.
 */
public class ReplJLineCompleter implements Completer {

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
        add(result, "select");
        add(result, "insert into");
        add(result, "update");
        add(result, "delete from");

        return result;
    }

    private static void addTx(List<SimpleCompleterCandidate> result, String base) {
        String[] optionList = { //
                "read only deferrable", //
                "read write", //
                "write preserve", //
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

    private static void add(List<SimpleCompleterCandidate> result, String candidateLine) {
        String words = candidateLine.trim();
        boolean end = candidateLine.endsWith(";");
        var candidate = new SimpleCompleterCandidate(words, end);
        result.add(candidate);
    }

    private void collectCandidate(String line, List<SimpleCompleterCandidate> candidateList, List<Candidate> candidates) {
        var inputWords = SimpleCompleterCandidate.toWordList(line);
        for (var candidate : candidateList) {
            var result = candidate.findCandidateWord(inputWords);
            if (result != null) {
                String word = result.word();
                boolean end = result.end();
                candidates.add(new Candidate(word, word, null, null, null, null, !end));
            }
        }
    }
}
