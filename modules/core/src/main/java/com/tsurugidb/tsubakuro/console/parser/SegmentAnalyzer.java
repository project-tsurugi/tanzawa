package com.tsurugidb.tsubakuro.console.parser;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tsubakuro.console.model.CallStatement;
import com.tsurugidb.tsubakuro.console.model.CommitStatement;
import com.tsurugidb.tsubakuro.console.model.CommitStatement.CommitStatus;
import com.tsurugidb.tsubakuro.console.model.ErroneousStatement.ErrorKind;
import com.tsurugidb.tsubakuro.console.model.Region;
import com.tsurugidb.tsubakuro.console.model.Regioned;
import com.tsurugidb.tsubakuro.console.model.SimpleStatement;
import com.tsurugidb.tsubakuro.console.model.SpecialStatement;
import com.tsurugidb.tsubakuro.console.model.StartTransactionStatement;
import com.tsurugidb.tsubakuro.console.model.StartTransactionStatement.ExclusiveMode;
import com.tsurugidb.tsubakuro.console.model.StartTransactionStatement.ReadWriteMode;
import com.tsurugidb.tsubakuro.console.model.Statement;
import com.tsurugidb.tsubakuro.console.model.Value;

/**
 * Analyzes {@link Segment} and returns the corresponding {@link Statement}.
 */
final class SegmentAnalyzer {

    private static final String K_WITH = "WITH";

    private static final String K_READ = "READ";

    private static final String K_DEFERRABLE = "DEFERRABLE";

    private static final String K_ONLY = "ONLY";

    private static final String K_AREA = "AREA";

    private static final String K_INCLUDE = "INCLUDE";

    private static final String K_WRITE = "WRITE";

    private static final String K_PRESERVE = "PRESERVE";

    private static final String K_EXECUTE = "EXECUTE";

    private static final String K_PRIOR = "PRIOR";

    private static final String K_EXCLUDING = "EXCLUDING";

    private static final String K_EXCLUDE = "EXCLUDE";

    private static final String K_IMMEDIATE = "IMMEDIATE";

    private static final String K_AS = "AS";

    private static final String K_WAIT = "WAIT";

    private static final String K_FOR = "FOR";

    private static final String K_START = "START";

    private static final String K_BEGIN = "BEGIN";

    private static final String K_TRANSACTION = "TRANSACTION";

    private static final String K_LONG = "LONG";

    private static final String K_COMMIT = "COMMIT";

    private static final String K_ROLLBACK = "ROLLBACK";

    private static final String K_CALL = "CALL";

    static final Logger LOG = LoggerFactory.getLogger(SegmentAnalyzer.class);

    static Statement analyze(@Nonnull Segment segment) throws ParseException {
        LOG.debug("analyze segment: {}", segment.getText()); //$NON-NLS-1$
        try {
            Statement result = new SegmentAnalyzer(segment).analyze();
            LOG.debug("analyze result: {}", result); //$NON-NLS-1$
            return result;
        } catch (ParseException e) {
            LOG.debug("analyze error: {} ({})", e.getMessage(), e.getErrorKind()); //$NON-NLS-1$
            throw e;
        }
    }

    private final Segment segment;

    private final TokenCursor cursor;

    private SegmentAnalyzer(Segment segment) {
        this.segment = segment;
        this.cursor = new TokenCursor(segment);
    }

    private Statement analyze() throws ParseException {
        if (testNext(TokenKind.END_OF_STATEMENT)) {
            LOG.trace("found statement delimiter -> empty statement"); //$NON-NLS-1$
            return new SimpleStatement(Statement.Kind.EMPTY, segment.getText(), getSegmentRegion());
        }
        if (testNext(K_START) || testNext(K_BEGIN)) {
            LOG.trace("found start/begin -> start transaction statement"); //$NON-NLS-1$
            var isBegin = testNext(K_BEGIN);
            cursor.consume(1);
            StartTransactionCandidate candidate = new StartTransactionCandidate();
            if (testNext(K_TRANSACTION)) {
                cursor.consume(1);
            } else if (testNext(K_LONG, K_TRANSACTION)) {
                candidate.transactionMode = cursor.region(0).wrap(StartTransactionStatement.TransactionMode.LONG);
                cursor.consume(2);
            } else if (!isBegin) {
                throw new ParseException(ErrorKind.UNEXPECTED_TOKEN, cursor.region(0), MessageFormat.format(
                        "unexpected token \"{0}\" ({1}): expected \"TRANSACTION\" or \"LONG\" \"TRANSACTION\"",
                        cursor.text(0),
                        cursor.token(0).getKind()));
            }
            return analyzeStartTransaction(candidate);
        }
        if (testNext(K_COMMIT)) {
            LOG.trace("found commit -> commit statement"); //$NON-NLS-1$
            cursor.consume(1);
            return analyzeCommit();
        }
        if (testNext(K_ROLLBACK)) {
            LOG.trace("found rollback -> rollback statement"); //$NON-NLS-1$
            cursor.consume(1);
            return analyzeRollback();
        }
        if (testNext(K_CALL)) {
            LOG.trace("found call -> (try) call statement"); //$NON-NLS-1$
            cursor.consume(1);
            return analyzeCall();
        }
        if (testNext(TokenKind.SPECIAL_COMMAND)) {
            LOG.trace("found special command -> special statement"); //$NON-NLS-1$
            var region = cursor.region(0);
            var text = cursor.text(0);
            cursor.consume(1);
            if (!text.startsWith("\\")) {
                throw new IllegalArgumentException("special command must start with \"\\\"");
            }
            return new SpecialStatement(segment.getText(), getSegmentRegion(), region.wrap(text.substring(1)));
        }
        LOG.trace("not found statement signature -> generic SQL statement"); //$NON-NLS-1$
        return new SimpleStatement(Statement.Kind.GENERIC, segment.getText(), getSegmentRegion());
    }

    private Statement analyzeStartTransaction(StartTransactionCandidate candidate) throws ParseException {
        while (true) {
            if (testNext(TokenKind.END_OF_STATEMENT)) {
                LOG.trace("find end of start transaction statement"); //$NON-NLS-1$
                cursor.consume(1);
                break;
            }
            if (testNext(K_READ, K_ONLY, K_DEFERRABLE)) {
                LOG.trace("found read only deferrable"); //$NON-NLS-1$
                var region = cursor.region(0, 2);
                cursor.consume(3);
                checkReadWriteModeOption(candidate, region);
                candidate.readWriteMode = region.wrap(ReadWriteMode.READ_ONLY_DEFERRABLE);
                continue;
            }
            if (testNext(K_READ, K_ONLY)) {
                LOG.trace("found read only"); //$NON-NLS-1$
                var region = cursor.region(0, 1);
                cursor.consume(2);
                checkReadWriteModeOption(candidate, region);
                candidate.readWriteMode = region.wrap(ReadWriteMode.READ_ONLY);
                continue;
            }
            if (testNext(K_READ, K_WRITE)) {
                LOG.trace("found read only"); //$NON-NLS-1$
                var region = cursor.region(0, 1);
                cursor.consume(2);
                checkReadWriteModeOption(candidate, region);
                candidate.readWriteMode = region.wrap(ReadWriteMode.READ_WRITE);
                continue;
            }
            if (testNext(K_READ, K_AREA)) {
                LOG.trace("found read area"); //$NON-NLS-1$
                checkReadAreaOption(candidate, cursor.region(0, 1));
                cursor.consume(2);
                boolean sawLimit = false;
                while (true) {
                    if (testNext(K_INCLUDE)) {
                        sawLimit = true;
                        checkReadAreaIncludeOption(candidate, cursor.region(0));
                        cursor.consume(1);
                        candidate.readAreaInclude = consumeTableList();
                        continue;
                    }
                    if (testNext(K_EXCLUDE)) {
                        sawLimit = true;
                        checkReadAreaExcludeOption(candidate, cursor.region(0));
                        cursor.consume(1);
                        candidate.readAreaExclude = consumeTableList();
                        continue;
                    }
                    break;
                }
                if (sawLimit == false) {
                    throw new ParseException(ErrorKind.UNEXPECTED_TOKEN, cursor.region(0), MessageFormat.format(
                            "unexpected token \"{0}\" ({1}): expected \"INCLUDE\" or \"EXCLUDE\"",
                            cursor.text(0),
                            cursor.token(0).getKind()));
                }
                continue;
            }
            if (testNext(K_WRITE, K_PRESERVE)) {
                LOG.trace("found write preserve"); //$NON-NLS-1$
                checkWritePreserveOption(candidate, cursor.region(0, 1));
                cursor.consume(2);
                candidate.writePreserve = consumeTableList();
                continue;
            }
            if (testNext(K_EXECUTE, K_PRIOR, K_IMMEDIATE)) {
                LOG.trace("found execute prior immediate"); //$NON-NLS-1$
                var region = cursor.region(0, 2);
                cursor.consume(3);
                checkExclusiveModeOption(candidate, region);
                candidate.exclusiveMode = region.wrap(ExclusiveMode.PRIOR_IMMEDIATE);
                continue;
            }
            if (testNext(K_EXECUTE, K_PRIOR)) {
                LOG.trace("found execute prior"); //$NON-NLS-1$
                var region = cursor.region(0, 1);
                cursor.consume(2);
                if (testNext(K_DEFERRABLE)) {
                    region = region.union(cursor.region(0));
                    cursor.consume(1);
                }
                checkExclusiveModeOption(candidate, region);
                candidate.exclusiveMode = region.wrap(ExclusiveMode.PRIOR_DEFERRABLE);
                continue;
            }
            if (testNext(K_EXECUTE, K_EXCLUDING, K_IMMEDIATE) || testNext(K_EXECUTE, K_EXCLUDE, K_IMMEDIATE)) {
                LOG.trace("found execute excluding immediate"); //$NON-NLS-1$
                var region = cursor.region(0, 2);
                cursor.consume(3);
                checkExclusiveModeOption(candidate, region);
                candidate.exclusiveMode = region.wrap(ExclusiveMode.EXCLUDING_IMMEDIATE);
                continue;
            }
            if (testNext(K_EXECUTE, K_EXCLUDING) || testNext(K_EXECUTE, K_EXCLUDE)) {
                LOG.trace("found execute excluding"); //$NON-NLS-1$
                var region = cursor.region(0, 1);
                cursor.consume(2);
                if (testNext(K_DEFERRABLE)) {
                    region = region.union(cursor.region(0));
                    cursor.consume(1);
                }
                checkExclusiveModeOption(candidate, region);
                candidate.exclusiveMode = region.wrap(ExclusiveMode.EXCLUDING_DEFERRABLE);
                continue;
            }
            if (testNext(K_AS)) {
                LOG.trace("found as"); //$NON-NLS-1$
                checkTransactionLabelOption(candidate, cursor.region(0, 1));
                cursor.consume(1);
                candidate.label = consumeIdentifierOrString();
                continue;
            }
            if (testNext(K_WITH)) {
                LOG.trace("found with"); //$NON-NLS-1$
                checkTransactinoPropertiesOption(candidate, cursor.region(0, 1));
                cursor.consume(1);
                candidate.properties = consumeKeyValuePairList();
                continue;
            }
            throw new ParseException(ErrorKind.UNEXPECTED_TOKEN, cursor.region(0), MessageFormat.format(
                    "unexpected token \"{0}\" ({1}): expected transaction option or end-of-statement",
                    cursor.text(0),
                    cursor.token(0).getKind()));
        }
        return new StartTransactionStatement(
                segment.getText(),
                getSegmentRegion(),
                candidate.transactionMode,
                candidate.readWriteMode,
                candidate.exclusiveMode,
                candidate.writePreserve,
                candidate.readAreaInclude,
                candidate.readAreaExclude,
                candidate.label,
                candidate.properties);
    }

    private List<Regioned<String>> consumeTableList() throws ParseException {
        LOG.trace("analyzing table list"); //$NON-NLS-1$
        if (testNext(TokenKind.LEFT_PAREN)) {
            cursor.consume(1);
            if (testNext(TokenKind.RIGHT_PAREN)) {
                cursor.consume(1);
                return List.of();
            }
            var results = consumeTableListBody();
            expectNext(TokenKind.RIGHT_PAREN, ")");
            cursor.consume(1);
            return results;
        }
        return consumeTableListBody();
    }

    private List<Regioned<String>> consumeTableListBody() throws ParseException {
        List<Regioned<String>> results = new ArrayList<>();
        results.add(consumeNameOrString());
        while (testNext(TokenKind.COMMA)) {
            cursor.consume(1);
            results.add(consumeNameOrString());
        }
        return results;
    }

    private Map<Regioned<String>, Optional<Regioned<Value>>> consumeKeyValuePairList() throws ParseException {
        LOG.trace("analyzing key-value pairs"); //$NON-NLS-1$
        if (testNext(TokenKind.LEFT_PAREN)) {
            cursor.consume(1);
            if (testNext(TokenKind.RIGHT_PAREN)) {
                cursor.consume(1);
                return Map.of();
            }
            var result = consumeKeyValuePairListBody();
            expectNext(TokenKind.RIGHT_PAREN, ")");
            cursor.consume(1);
            return result;
        }
        return consumeKeyValuePairListBody();
    }

    private Map<Regioned<String>, Optional<Regioned<Value>>> consumeKeyValuePairListBody() throws ParseException {
        var results = new LinkedHashMap<Regioned<String>, Optional<Regioned<Value>>>();
        addKeyValuePair(results, consumeKeyValuePair());
        while (testNext(TokenKind.COMMA)) {
            cursor.consume(1);
            addKeyValuePair(results, consumeKeyValuePair());
        }
        return results;
    }

    private Map.Entry<Regioned<String>, Optional<Regioned<Value>>> consumeKeyValuePair() throws ParseException {
        var key = consumeNameOrString();
        Optional<Regioned<Value>> value = Optional.empty();
        if (testNext(TokenKind.EQUAL)) {
            cursor.consume(1);
            value = Optional.of(consumeIdentifierOrLiteral());
        }
        return Map.entry(key, value);
    }

    private static void addKeyValuePair(
            Map<Regioned<String>, Optional<Regioned<Value>>> destination,
            Map.Entry<Regioned<String>, Optional<Regioned<Value>>> pair) throws ParseException {
        var existing = destination.putIfAbsent(pair.getKey(), pair.getValue());
        if (existing != null) {
            throw new ParseException(
                    ErrorKind.CONFLICT_PROPERTIES_KEY,
                    pair.getKey().getRegion(),
                    MessageFormat.format(
                            "property \"{0}\" is duplicated",
                            pair.getKey().getValue()));
        }
    }

    private Statement analyzeCommit() throws ParseException {
        var status = consumeCommitStatus();
        expectEndOfStatement();
        return new CommitStatement(segment.getText(), getSegmentRegion(), status);
    }

    private Regioned<CommitStatus> consumeCommitStatus() throws ParseException {
        if (testNext(K_WAIT)) {
            LOG.trace("found wait for"); //$NON-NLS-1$
            cursor.consume(1);
            if (testNext(K_FOR)) {
                cursor.consume(1);
            }
            Regioned<String> statusName = consumeIdentifierOrString();
            try {
                return statusName.getRegion().wrap(
                        CommitStatus.valueOf(statusName.getValue().toUpperCase(Locale.ENGLISH)));
            } catch (@SuppressWarnings("unused") IllegalArgumentException e) {
                throw new ParseException(ErrorKind.UNKNOWN_COMMIT_STATUS, cursor.region(0), MessageFormat.format(
                        "unknown commit status \"{0}\": expected one of {1}",
                        statusName,
                        Arrays.stream(CommitStatus.values())
                            .map(CommitStatus::name)
                            .collect(Collectors.joining(", "))));
            }
        }
        return null;
    }

    private Statement analyzeRollback() throws ParseException {
        expectEndOfStatement();
        return new SimpleStatement(Statement.Kind.ROLLBACK, segment.getText(), getSegmentRegion());
    }

    private Statement analyzeCall() {
        try {
            var name = consumeName();
            var arguments = consumeProcedureArgumentList();
            expectEndOfStatement();
            return new CallStatement(segment.getText(), getSegmentRegion(), name, arguments);
        } catch (ParseException e) {
            LOG.debug("call statement is not a constant form; recongnize as generic statement", e); //$NON-NLS-1$
            return new SimpleStatement(Statement.Kind.GENERIC, segment.getText(), getSegmentRegion());
        }
    }

    private List<Regioned<Value>> consumeProcedureArgumentList() throws ParseException {
        LOG.trace("analyzing call argument list"); //$NON-NLS-1$
        expectNext(TokenKind.LEFT_PAREN, "("); //$NON-NLS-1$
        cursor.consume(1);
        if (testNext(TokenKind.RIGHT_PAREN)) {
            cursor.consume(1);
            return List.of();
        }
        List<Regioned<Value>> results = new ArrayList<>();
        results.add(consumeNameOrLiteral());
        while (testNext(TokenKind.COMMA)) {
            cursor.consume(1);
            results.add(consumeNameOrLiteral());
        }
        expectNext(TokenKind.RIGHT_PAREN, ")"); //$NON-NLS-1$
        cursor.consume(1);
        return results;
    }

    private Regioned<String> consumeNameOrString() throws ParseException {
        if (testNext(TokenKind.REGULAR_IDENTIFIER) || testNext(TokenKind.DELIMITED_IDENTIFIER)) {
            return consumeName();
        }
        if (testNext(TokenKind.CHARACTER_STRING_LITERAL)) {
            return consumeCharacterStringLiteral();
        }
        TokenInfo token = cursor.token(0);
        throw new ParseException(ErrorKind.UNEXPECTED_TOKEN, cursor.region(0), MessageFormat.format(
                "unexpected token \"{0}\" ({1}): expected name or character string",
                segment.getText(token),
                token.getKind()));
    }

    private Regioned<String> consumeIdentifierOrString() throws ParseException {
        if (testNext(TokenKind.REGULAR_IDENTIFIER)) {
            return consumeIdentifier();
        }
        if (testNext(TokenKind.CHARACTER_STRING_LITERAL)) {
            return consumeCharacterStringLiteral();
        }
        TokenInfo token = cursor.token(0);
        throw new ParseException(ErrorKind.UNEXPECTED_TOKEN, cursor.region(0), MessageFormat.format(
                "unexpected token \"{0}\" ({1}): expected identifier or character string",
                segment.getText(token),
                token.getKind()));
    }

    private Regioned<Value> consumeNameOrLiteral() throws ParseException {
        if (testNext(TokenKind.REGULAR_IDENTIFIER) || testNext(TokenKind.DELIMITED_IDENTIFIER)) {
            return consumeName().map(Value::of);
        }
        return consumeLiteral();
    }

    private Regioned<Value> consumeIdentifierOrLiteral() throws ParseException {
        if (testNext(TokenKind.REGULAR_IDENTIFIER)) {
            return consumeIdentifier().map(Value::of);
        }
        return consumeLiteral();
    }

    private Regioned<Value> consumeLiteral() throws ParseException {
        if (testNext(TokenKind.CHARACTER_STRING_LITERAL)) {
            return consumeCharacterStringLiteral().map(Value::of);
        }
        if (testNext(TokenKind.NUMERIC_LITERAL)) {
            var value = new BigDecimal(cursor.text(0));
            var result = new Regioned<>(Value.of(value), cursor.region(0));
            cursor.consume(1);
            return result;
        }
        if (testNext(TokenKind.PLUS) || testNext(TokenKind.MINUS)) {
            var sign = cursor.token(0);
            cursor.consume(1);
            if (testNext(TokenKind.NUMERIC_LITERAL)) {
                var value = new BigDecimal(cursor.text(0));
                if (sign.getKind() == TokenKind.MINUS) {
                    value = value.negate();
                }
                var result = new Regioned<>(Value.of(value), cursor.region(0));
                cursor.consume(1);
                return result;
            }
            throw new ParseException(ErrorKind.UNEXPECTED_TOKEN, cursor.region(0), MessageFormat.format(
                    "unexpected token \"{0}\" ({1}): expected a number",
                    cursor.text(0),
                    cursor.token(0).getKind()));
        }
        if (testNext(TokenKind.TRUE_LITERAL)) {
            var result = new Regioned<>(Value.of(true), cursor.region(0));
            cursor.consume(1);
            return result;
        }
        if (testNext(TokenKind.FALSE_LITERAL)) {
            var result = new Regioned<>(Value.of(false), cursor.region(0));
            cursor.consume(1);
            return result;
        }
        if (testNext(TokenKind.NULL_LITERAL)) {
            var result = new Regioned<>(Value.of(), cursor.region(0));
            cursor.consume(1);
            return result;
        }
        throw new ParseException(ErrorKind.UNEXPECTED_TOKEN, cursor.region(0), MessageFormat.format(
                "unexpected token \"{0}\" ({1}): expected a literal",
                cursor.text(0),
                cursor.token(0).getKind()));
    }

    private Regioned<String> consumeName() throws ParseException {
        StringBuilder buf = new StringBuilder();
        Regioned<String> first = consumeIdentifier();
        buf.append(first.getValue());
        Region region = first.getRegion();
        while (testNext(TokenKind.DOT)) {
            buf.append(".");
            cursor.consume(1);
            if (testNext(TokenKind.ASTERISK)) {
                buf.append("*");
                region = region.union(cursor.region(0));
                cursor.consume(1);
                break;
            }
            Regioned<String> next = consumeIdentifier();
            buf.append(next.getValue());
            region = region.union(next.getRegion());
        }
        return region.wrap(buf.toString());
    }

    private Regioned<String> consumeIdentifier() throws ParseException {
        if (testNext(TokenKind.REGULAR_IDENTIFIER) || testNext(TokenKind.DELIMITED_IDENTIFIER)) {
            var region = cursor.region(0);
            var text = cursor.text(0);
            cursor.consume(1);
            return region.wrap(text);
        }
        throw new ParseException(ErrorKind.UNEXPECTED_TOKEN, cursor.region(0), MessageFormat.format(
                "unexpected token \"{0}\" ({1}): expected identifier",
                cursor.text(0),
                cursor.token(0).getKind()));
    }

    private Regioned<String> consumeCharacterStringLiteral() throws ParseException {
        if (testNext(TokenKind.CHARACTER_STRING_LITERAL)) {
            var region = cursor.region(0);
            var text = decodeCharacterString(cursor.text(0));
            cursor.consume(1);
            return region.wrap(text);
        }
        throw new ParseException(ErrorKind.UNEXPECTED_TOKEN, cursor.region(0), MessageFormat.format(
                "unexpected token \"{0}\" ({1}): expected character string",
                cursor.text(0),
                cursor.token(0).getKind()));
    }

    private static String decodeCharacterString(String text) {
        StringBuilder buf = new StringBuilder();
        if (text.length() < 2 || text.charAt(0) != '\'' || text.charAt(text.length() - 1) != '\'') {
            throw new IllegalArgumentException(MessageFormat.format(
                    "invalid character string: {0}", //$NON-NLS-1$
                    text));
        }
        boolean sawEscape = false;
        for (int i = 1, n = text.length() - 1; i < n; i++) {
            char c = text.charAt(i);
            if (sawEscape) {
                switch (c) {
                case 'n': buf.append('\n'); break;
                case 'r': buf.append('\r'); break;
                case 't': buf.append('\t'); break;
                default: buf.append(c); break;
                }
                sawEscape = false;
            } else if (c == '\\') {
                sawEscape = true;
            } else {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    private void expectNext(TokenKind kind, String symbol) throws ParseException {
        if (!testNext(kind)) {
            TokenInfo token = cursor.token(0);
            throw new ParseException(ErrorKind.UNEXPECTED_TOKEN, cursor.region(0), MessageFormat.format(
                    "unexpected token \"{0}\" ({1}): expected token is \"{2}\" ({3})",
                    segment.getText(token),
                    token.getKind(),
                    symbol,
                    kind));
        }
    }

    private void expectEndOfStatement() throws ParseException {
        if (!testNext(TokenKind.END_OF_STATEMENT)) {
            TokenInfo token = cursor.token(0);
            throw new ParseException(ErrorKind.UNEXPECTED_TOKEN, cursor.region(0), MessageFormat.format(
                    "unexpected token \"{0}\" ({1}): expected end of statement",
                    segment.getText(token),
                    token.getKind()));
        }
    }

    private void checkReadWriteModeOption(
            StartTransactionCandidate candidate, Region region) throws ParseException {
        if (candidate.readWriteMode != null) {
            LOG.trace("conflict read-write mode: {}", region);
            throw new ParseException(ErrorKind.CONFLICT_READ_WRITE_MODE_OPTION, region, MessageFormat.format(
                    "transaction read-write mode is already declared as \"{0}\"",
                    segment.getText(region).orElse("N/A")));
        }
    }

    private void checkExclusiveModeOption(
            StartTransactionCandidate candidate, Region region) throws ParseException {
        if (candidate.exclusiveMode != null) {
            LOG.trace("conflict execute mode: {}", region);
            throw new ParseException(ErrorKind.CONFLICT_EXCLUSIVE_MODE_OPTION, region, MessageFormat.format(
                    "transaction exclusive mode is already declared as \"{0}\"",
                    segment.getText(region).orElse("N/A")));
        }
    }

    private static void checkWritePreserveOption(
            StartTransactionCandidate candidate, Region region) throws ParseException {
        if (candidate.writePreserve != null) {
            throw new ParseException(ErrorKind.DUPLICATE_WRITE_PRESERVE_OPTION, region,
                    "write preserve is already declared");
        }
    }

    private static void checkReadAreaOption(
            StartTransactionCandidate candidate, Region region) throws ParseException {
        if (candidate.readAreaInclude != null || candidate.readAreaExclude != null) {
            throw new ParseException(ErrorKind.DUPLICATE_READ_AREA_OPTION, region,
                    "read area is already declared");
        }
    }

    private static void checkReadAreaIncludeOption(
            StartTransactionCandidate candidate, Region region) throws ParseException {
        if (candidate.readAreaInclude != null) {
            throw new ParseException(ErrorKind.DUPLICATE_READ_AREA_OPTION, region,
                    "read area include is already declared");
        }
    }

    private static void checkReadAreaExcludeOption(
            StartTransactionCandidate candidate, Region region) throws ParseException {
        if (candidate.readAreaExclude != null) {
            throw new ParseException(ErrorKind.DUPLICATE_READ_AREA_OPTION, region,
                    "read area exclude is already declared");
        }
    }

    private static void checkTransactionLabelOption(
            StartTransactionCandidate candidate, Region region) throws ParseException {
        if (candidate.label != null) {
            throw new ParseException(ErrorKind.DUPLICATE_TRANSACTION_LABEL_OPTION, region,
                    "transaction label is already declared");
        }
    }

    private static void checkTransactinoPropertiesOption(
            StartTransactionCandidate candidate, Region region) throws ParseException {
        if (candidate.properties != null) {
            throw new ParseException(ErrorKind.DUPLICATE_TRANSACTION_PROPERTIES_OPTION, region,
                    "transaction properties (\"WITH key=value, ...\") is already declared");
        }
    }

    private boolean testNext(TokenKind kind) {
        return testAt(kind, 0);
    }

    private boolean testNext(String keyword) {
        return testAt(keyword, 0);
    }

    private boolean testNext(String keyword, String... rest) {
        if (!testAt(keyword, 0)) {
            return false;
        }
        for (int i = 0; i < rest.length; i++) {
            if (!testAt(rest[i], i + 1)) {
                return false;
            }
        }
        return true;
    }

    private boolean testAt(TokenKind kind, int offset) {
        return cursor.lookahead(offset)
                .filter(it -> it.getKind() == kind)
                .isPresent();
    }

    private boolean testAt(String keyword, int offset) {
        return cursor.lookahead(offset)
                .filter(it -> it.getKind() == TokenKind.REGULAR_IDENTIFIER)
                .flatMap(segment::getText)
                .filter(it -> it.equalsIgnoreCase(keyword))
                .isPresent();
    }

    private Region getSegmentRegion() {
        return new Region(
                segment.getOffset(),
                segment.getText().length(),
                segment.getStartLine(),
                segment.getStartColumn());
    }
}
