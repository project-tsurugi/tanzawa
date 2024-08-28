/*
 * Copyright 2023-2024 Project Tsurugi.
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
package com.tsurugidb.tools.tgdump.cli;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.tsurugidb.tools.common.connection.BasicCredentialProvider;
import com.tsurugidb.tools.common.connection.ConnectionSettings;
import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;
import com.tsurugidb.tools.common.monitoring.MonitoringException;
import com.tsurugidb.tools.tgdump.core.engine.BasicDumpMonitor;
import com.tsurugidb.tools.tgdump.core.engine.BasicDumpSession;
import com.tsurugidb.tools.tgdump.core.engine.CompositeDumpMonitor;
import com.tsurugidb.tools.tgdump.core.engine.DumpEngine;
import com.tsurugidb.tools.tgdump.core.engine.DumpMonitor;
import com.tsurugidb.tools.tgdump.core.model.TransactionSettings;
import com.tsurugidb.tools.tgdump.profile.DumpProfileBundle;
import com.tsurugidb.tools.tgdump.profile.DumpProfileBundleLoader;
import com.tsurugidb.tsubakuro.channel.common.connection.NullCredential;
import com.tsurugidb.tsubakuro.exception.ServerException;
import com.tsurugidb.tsubakuro.sql.SqlClient;

/**
 * The program entry for Tsurugi Table Dump Tool ({@literal a.k.a.} {@code tgdump}}).
 * @see CommandArgumentSet
 */
public class Main {

    static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private final Printer printer;

    /**
     * Creates a new instance.
     */
    public Main() {
        this(new PrintStreamPrinter(System.out));
    }

    /**
     * Creates a new instance.
     * @param printer the message printer
     */
    public Main(@Nonnull Printer printer) {
        Objects.requireNonNull(printer);
        this.printer = printer;
    }

    /**
     * Program entry for Tsurugi Dump Tool.
     * <p>
     * This operation will terminate the current Java VM.
     * </p>
     * <p>
     * This operation may terminate the current Java VM.
     * </p>
     * @param args the program arguments
     * @see #execute(String...)
     */
    public static void main(@Nonnull String... args) {
        Objects.requireNonNull(args);
        var app = new Main();
        System.exit(app.execute(args));
    }

    /**
     * Program entry for Tsurugi Dump Tool, without shutdown the Java VM.
     * @param args the program argument
     * @return the exit status code
     */
    public int execute(@Nonnull String... args) {
        Objects.requireNonNull(args);
        CommandArgumentSet arguments;
        try {
            arguments = parseArguments(args);
        } catch (ParameterException e) {
            LOG.debug("error occurred while analyzing command options", e); //$NON-NLS-1$
            LOG.error("invalid_parameter: {}", DiagnosticUtil.getMessage(e));
            return Constants.EXIT_STATUS_PARAMETER_ERROR;
        }
        return execute(arguments);
    }

    /**
     * Parses the command arguments.
     * @param args the command arguments
     * @return the parsed command configuration
     * @throws ParameterException if the arguments are wrong for the command
     */
    protected CommandArgumentSet parseArguments(@Nonnull String... args) {
        Objects.requireNonNull(args);
        var result = new CommandArgumentSet();
        var analyzer = getCommandAnalyzerFor(result);
        analyzer.parse(args);
        return result;
    }

    /**
     * Program entry for Tsurugi Dump Tool, without shutdown the Java VM.
     * @param arguments the parsed command arguments
     * @return the exit status code
     */
    protected int execute(@Nonnull CommandArgumentSet arguments) {
        Objects.requireNonNull(arguments);
        if (arguments.isPrintHelp()) {
            printHelp(arguments.getProfileBundleLoader());
            return Constants.EXIT_STATUS_OK;
        }
        if (arguments.isPrintVersion()) {
            CommandUtil.printVersion(printer, Main.class.getClassLoader());
            return Constants.EXIT_STATUS_OK;
        }
        if (arguments.isVerbose()) {
            CommandUtil.printArgumentSet(printer, arguments);
        }
        try (var monitor = CommandUtil.createMonitor(arguments.getMonitorOutputPath())) {
            monitor.onStart();
            try {
                var dumpMonitor = new CompositeDumpMonitor(List.of(
                        new ConsoleDumpMonitor(printer, arguments.isVerbose()),
                        new BasicDumpMonitor(monitor)));
                executeBody(dumpMonitor, arguments);
            } catch (DiagnosticException e) {
                LOG.debug("diagnostic error", e);
                LOG.error("{} - {}", e.getDiagnosticCode().getTag(), e.getMessage());
                monitor.onFailure(e);
                return Constants.EXIT_STATUS_OPERATION_ERROR;
            } catch (IOException e) {
                LOG.error("I/O error was occurred", e);
                monitor.onFailure(e, CliDiagnosticCode.IO_ERROR, List.of(DiagnosticUtil.getMessage(e)));
                return Constants.EXIT_STATUS_OPERATION_ERROR;
            } catch (InterruptedException e) {
                LOG.debug("interrupted", e);
                LOG.error("interrupted");
                monitor.onFailure(e, CliDiagnosticCode.INTERRUPTED, List.of());
                return Constants.EXIT_STATUS_INTERRUPTED;
            } catch (RuntimeException e) {
                LOG.error("internal error", e);
                monitor.onFailure(e, CliDiagnosticCode.INTERNAL, List.of(DiagnosticUtil.getMessage(e)));
                return Constants.EXIT_STATUS_INTERNAL_ERROR;
            }
            monitor.onSuccess();
        } catch (IOException | MonitoringException e) {
            LOG.error("error occurred while monitoring dump operations", e);
            return Constants.EXIT_STATUS_MONITOR_ERROR;
        }
        return Constants.EXIT_STATUS_OK;
    }

    void printHelp(DumpProfileBundleLoader bundleLoader) {
        var analyzer = getCommandAnalyzerFor(new CommandArgumentSet());
        analyzer.usage();
        try {
            var bundle = bundleLoader.load();
            printBuiltinProfiles(bundle);
        } catch (DiagnosticException e) {
            // ignore error
            LOG.debug("error occurred while loading built-in profiles", e); //$NON-NLS-1$
        }
    }

    private void printBuiltinProfiles(DumpProfileBundle profileBundle) {
        printer.printf("  %s:", "Available built-in dump profiles"); //$NON-NLS-1$
        for (var name : profileBundle.getProfileNames()) {
            var profile = profileBundle.findProfile(name).get();
            printer.printf("    --profile %s", name); //$NON-NLS-1$
            profile.getLocalizedDescription(Locale.getDefault())
                    .or(profile::getDescription)
                    .ifPresent(it -> printer.printf("      %s", it)); //$NON-NLS-1$
        }
    }

    void executeBody(DumpMonitor monitor, CommandArgumentSet args)
            throws DiagnosticException, InterruptedException, IOException {
        var connectionSettings = ConnectionSettings.newBuilder()
                .withEndpointUri(args.getConnectionUri())
                .withApplicationName(Constants.APPLICATION_NAME)
                .withSessionLabel(args.getConnectionLabel())
                .withEstablishTimeout(Duration.ofMillis(args.getConnectionTimeoutMillis()))
                .withCredentialProviders(List.of(new BasicCredentialProvider("default", NullCredential.INSTANCE)))
                .build();
        var transactionSettings = TransactionSettings.newBuilder()
                .withType(args.getTransactionType())
                .withLabel(args.getTransactionLabel())
                .build();
        var engine = new DumpEngine(args.getNumberOfWorkerThreads());
        var profile = CommandUtil.loadProfile(args.getProfileBundleLoader(), args.getProfile());
        var targets = CommandUtil.prepareDestination(
                args.getTargetSelector(), args.getDestinationPath(), args.getTableNames());
        try (
            var connection = args.getConnectionProvider().connect(connectionSettings);
            var sql = SqlClient.attach(connection);
            var session = new BasicDumpSession(sql, transactionSettings, profile, true);
        ) {
            engine.execute(monitor, session, targets);
        } catch (ServerException e) {
            throw new CliException(CliDiagnosticCode.SERVER_ERROR,
                    List.of(DiagnosticUtil.getMessage(e)),
                    e);
        }
    }

    static JCommander getCommandAnalyzerFor(CommandArgumentSet result) {
        var analyzer = JCommander.newBuilder()
                .programName(Constants.APPLICATION_NAME)
                .addObject(result)
                .build();
        return analyzer;
    }
}
