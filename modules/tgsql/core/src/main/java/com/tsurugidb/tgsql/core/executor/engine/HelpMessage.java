package com.tsurugidb.tgsql.core.executor.engine;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tgsql.core.executor.engine.command.SpecialCommand;
import com.tsurugidb.tgsql.core.model.Regioned;
import com.tsurugidb.tgsql.core.model.SpecialStatement;

/**
 * Resource bundle of help messages.
 */
public class HelpMessage {

    static final Logger LOG = LoggerFactory.getLogger(HelpMessage.class);

    /**
     * The resource path of help messages.
     */
    public static final String BUNDLE_PATH = "/com/tsurugidb/tgsql/help"; //$NON-NLS-1$

    /**
     * The key prefix of messages for individual commands.
     */
    public static final String KEY_PREFIX_COMMAND = "command."; //$NON-NLS-1$

    /**
     * The key prefix of messages for individual special commands.
     */
    public static final String KEY_PREFIX_SPECIAL_COMMAND = "special."; //$NON-NLS-1$

    /**
     * The key of message for help command itself.
     */
    public static final String KEY_ROOT = "help"; //$NON-NLS-1$

    /**
     * The key of message for command is unrecognized.
     */
    public static final String KEY_NOT_FOUND = "unrecognized"; //$NON-NLS-1$

    private static final String PREFIX_REFERENCE = "@"; //$NON-NLS-1$

    private final Properties messages;

    private final List<String> availableSequences;

    /**
     * Creates a new instance.
     * @param messages the message bundle
     */
    public HelpMessage(@Nonnull Properties messages) {
        Objects.requireNonNull(messages);
        this.messages = new Properties();
        this.messages.putAll(messages);
        if (!this.messages.containsKey(KEY_ROOT)) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "message bundle must contain the root help message ({0})",
                    KEY_ROOT));
        }
        this.availableSequences = messages.keySet().stream()
                .filter(it -> it instanceof String)
                .map(it -> (String) it)
                .filter(it -> it.startsWith(KEY_PREFIX_COMMAND))
                .map(it -> it.substring(KEY_PREFIX_COMMAND.length()))
                .map(it -> it.replace('.', ' ').toUpperCase(Locale.ENGLISH))
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Creates a new instance.
     * @param rootMessage the root help message
     */
    public HelpMessage(@Nonnull String rootMessage) {
        Objects.requireNonNull(rootMessage);
        this.messages = new Properties();
        this.availableSequences = List.of();
        this.messages.setProperty(KEY_ROOT, rootMessage);
    }

    /**
     * Loads help message bundle from the class-path resource.
     * @param locale the bundle locale name (e.g. "en", "ja")
     * @return the created {@link HelpMessage} bundle
     * @throws IOException if I/O error occurred while loading bundle resource
     * @see #BUNDLE_PATH
     */
    public static HelpMessage load(@Nullable String locale) throws IOException {
        LOG.debug("loading help message bundle: {}", locale); //$NON-NLS-1$
        var resource = findBundle(locale);
        if (resource == null) {
            throw new FileNotFoundException(MessageFormat.format(
                    "help message bundle is not found: {0}",
                    String.format("%s.properties", BUNDLE_PATH))); //$NON-NLS-1$
        }
        LOG.debug("found help message bundle: {}", resource); //$NON-NLS-1$
        Properties properties = new Properties();
        try (
                var input = resource.openStream();
                var reader = new InputStreamReader(input, StandardCharsets.UTF_8);
        ) {
            properties.load(reader);
        }
        return new HelpMessage(properties);
    }

    private static @Nullable URL findBundle(String locale) {
        if (locale != null) {
            LOG.debug("searching for message bundle: locale={}", locale);  //$NON-NLS-1$
            var resource = HelpMessage.class.getResource(
                    String.format("%s_%s.properties", BUNDLE_PATH, locale)); //$NON-NLS-1$
            if (resource != null) {
                return resource;
            }
        }
        LOG.debug("searching for message bundle: locale=<default>");  //$NON-NLS-1$
        var resource = HelpMessage.class.getResource(String.format("%s.properties", BUNDLE_PATH)); //$NON-NLS-1$
        return resource;
    }

    /**
     * Returns help message keys.
     * @return message keys
     */
    public Collection<String> getKeys() {
        return messages.stringPropertyNames();
    }

    /**
     * Returns a help message.
     * @return the help message
     */
    public List<String> find() {
        return find(List.of());
    }

    /**
     * Returns a help message for the help command.
     * @param statement the help command
     * @return the help message
     */
    public List<String> find(@Nonnull SpecialStatement statement) {
        Objects.requireNonNull(statement);
        return find(statement.getCommandOptions().stream()
                .map(Regioned::getValue)
                .collect(Collectors.toList()));
    }

    /**
     * Returns a help message for the command sequence.
     * @param sequence the command sequence
     * @return the help message
     */
    public List<String> find(@Nonnull List<String> sequence) {
        Objects.requireNonNull(sequence);
        var message = find0(sequence);
        if (sequence.isEmpty()) {
            var result = new ArrayList<String>(message.size() + availableSequences.size());
            result.addAll(message);
            result.addAll(availableSequences);
            return result;
        }
        return message;
    }

    private List<String> find0(List<String> sequence) {
        LOG.debug("find help message: {}", sequence);
        String key = sequence.stream()
                .sequential()
                .map(it -> it.toLowerCase(Locale.ENGLISH))
                .collect(Collectors.joining(".")); //$NON-NLS-1$
        if (key.isEmpty()) {
            key = KEY_ROOT;
        } else {
            if (key.startsWith("\\")) {
                var list = SpecialCommand.findCommand(key.substring(1));
                if (!list.isEmpty()) {
                    var result = new ArrayList<String>();
                    for (var command : list) {
                        String commandName = command.command().getCommandName();
                        result.addAll(findForSpecialCommand(commandName));
                    }
                    return result;
                }
            }

            key = KEY_PREFIX_COMMAND + key;
        }
        return find0(key);
    }

    private List<String> find0(String key) {
        var value = messages.getProperty(key);
        if (value == null) {
            value = messages.getProperty(KEY_NOT_FOUND, "unrecognized help command.");
        } else if (value.startsWith(PREFIX_REFERENCE)) {
            return find0(List.of(value.substring(PREFIX_REFERENCE.length()).trim()));
        }
        return Arrays.asList(value.split("\r?\n"));
    }

    /**
     * Returns a help message for the special command.
     * @param commandName special command name
     * @return the help message
     */
    public List<String> findForSpecialCommand(String commandName) {
        String key = KEY_PREFIX_SPECIAL_COMMAND + commandName;
        return find0(key);
    }
}
