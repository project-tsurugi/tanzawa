package com.tsurugidb.console.cli.repl.jline;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

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

    private static List<List<String>> specialCommandCandidateList = null;

    private void completeSpecialCommand(String line, List<Candidate> candidates) {
        if (specialCommandCandidateList == null) {
            specialCommandCandidateList = SpecialCommand.getCompleterCandidateList();
        }
        collectCandidate(line, specialCommandCandidateList, candidates);
    }

    private static List<List<String>> sqCandidateList = null;

    private void completeSqlCommand(String line, List<Candidate> candidates) {
        // FIXME use SQL parser

        if (sqCandidateList == null) {
            sqCandidateList = getSqCandidateList();
        }
        collectCandidate(line, sqCandidateList, candidates);
    }

    private static List<List<String>> getSqCandidateList() {
        var result = new ArrayList<List<String>>();

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

    private static void addTx(List<List<String>> result, String base) {
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

    private static void add(List<List<String>> result, String candidate) {
        result.add(List.of(toWordList(candidate.trim().toLowerCase(Locale.ENGLISH))));
    }

    private void collectCandidate(String line, List<List<String>> candidateList, List<Candidate> candidates) {
        var inputWords = toWordList(line);
        for (var candidateWords : candidateList) {
            String word = getCandidateWord(inputWords, candidateWords);
            if (word != null) {
                boolean complete = word.endsWith(";");
                candidates.add(new Candidate(word, word, null, null, null, null, !complete));
            }
        }
    }

    /* private */ static String[] toWordList(String line) {
        String[] words = line.split("[ \t]+", -1); //$NON-NLS-1$
        return words;
    }

    @Nullable
    /* private */ static String getCandidateWord(String[] inputWords, List<String> candidateWords) {
        if (inputWords.length > candidateWords.size()) {
            return null;
        }
        for (int i = 0; i < inputWords.length; i++) {
            String inputWord = inputWords[i].toLowerCase(Locale.ENGLISH);
            String candidate = candidateWords.get(i);
            if (inputWord.equals(candidate)) {
                if (i == inputWords.length - 1) {
                    return candidate;
                }
                continue;
            }

            if (i == inputWords.length - 1) {
                if (candidate.startsWith(inputWord)) {
                    return candidate;
                }
            }
            return null;
        }

        int i = inputWords.length;
        if (i < candidateWords.size()) {
            return candidateWords.get(i);
        }
        return null;
    }
}
