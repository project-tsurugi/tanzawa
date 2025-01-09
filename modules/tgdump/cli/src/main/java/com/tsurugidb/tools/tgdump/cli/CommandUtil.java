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

import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tsurugidb.tools.common.diagnostic.DiagnosticException;
import com.tsurugidb.tools.common.diagnostic.DiagnosticUtil;
import com.tsurugidb.tools.common.monitoring.CompositeMonitor;
import com.tsurugidb.tools.common.monitoring.JsonMonitor;
import com.tsurugidb.tools.common.monitoring.LoggingMonitor;
import com.tsurugidb.tools.common.monitoring.Monitor;
import com.tsurugidb.tools.common.util.LibraryVersion;
import com.tsurugidb.tools.tgdump.core.engine.DumpTargetSelector;
import com.tsurugidb.tools.tgdump.core.model.DumpProfile;
import com.tsurugidb.tools.tgdump.core.model.DumpTarget;
import com.tsurugidb.tools.tgdump.profile.DumpProfileBundleLoader;
import com.tsurugidb.tsubakuro.client.ServiceClientCollector;
import com.tsurugidb.tsubakuro.sql.SqlClient;

/**
 * Utilities about Tsurugi Dump Tool.
 */
final class CommandUtil {

    private static final String SELF_MODULE_NAME = "tanzawa-tgdump-cli"; //$NON-NLS-1$

    private static final String TSUBAKURO_MODULE_NAME = "tsubakuro-session"; //$NON-NLS-1$

    private static final String TSUBAKURO_PRODUCT_LABEL = "Tsubakuro"; //$NON-NLS-1$

    static final Logger LOG = LoggerFactory.getLogger(Main.class); // use Main log-name

    private CommandUtil() {
        return;
    }

    static void printVersion(@Nonnull Printer printer, @Nullable ClassLoader loader) {
        Objects.requireNonNull(printer);

        printLibraryVersion(printer, loader, SELF_MODULE_NAME, Constants.APPLICATION_NAME);
        printLibraryVersion(printer, loader, TSUBAKURO_MODULE_NAME, TSUBAKURO_PRODUCT_LABEL);

        var smv = ServiceClientCollector.findServiceMessageVersion(SqlClient.class);
        printer.printf("Service message version: %s", smv.orElse("N/A"));
    }

    private static void printLibraryVersion(Printer printer, ClassLoader loader, String libraryName, String label) {
        try {
            var myself = LibraryVersion.loadByName(libraryName, loader);
            printer.printf("%s: %s", label, myself.getBuildVersion().orElse("N/A"));
        } catch (IOException e) {
            LOG.warn("cannot extract library version: {}", libraryName, e);
        }
    }

    static void printArgumentSet(@Nonnull Printer printer, @Nonnull CommandArgumentSet args) {
        Objects.requireNonNull(printer);
        Objects.requireNonNull(args);

        // dump core settings
        printArgument(printer, "(positional)", args.getTableNames());
        printArgument(printer, "--sql", args.isQueryMode()); //$NON-NLS-1$
        printArgument(printer, "--single", args.isSingleMode()); //$NON-NLS-1$
        printArgument(printer, "--to", args.getDestinationPath()); //$NON-NLS-1$
        printArgument(printer, "--profile", args.getProfile()); //$NON-NLS-1$

        // connection settings
        printArgument(printer, "--connection", args.getConnectionUri()); //$NON-NLS-1$
        printArgument(printer, "--connection-label", args.getConnectionLabel()); //$NON-NLS-1$
        printArgument(printer, "--connection-timeout", args.getConnectionTimeoutMillis()); //$NON-NLS-1$

        // transaction settings
        printArgument(printer, "--transaction", args.getTransactionType()); //$NON-NLS-1$
        printArgument(printer, "--transaction-label", args.getTransactionLabel()); //$NON-NLS-1$
        printArgument(printer, "--threads", args.getNumberOfWorkerThreads()); //$NON-NLS-1$

        // information settings
        printArgument(printer, "--verbose", args.isVerbose()); //$NON-NLS-1$
        printArgument(printer, "--monitor", args.getMonitorOutputPath()); //$NON-NLS-1$

        // special options
        printArgument(printer, "--help", args.isPrintHelp()); //$NON-NLS-1$
        printArgument(printer, "--version", args.isPrintVersion()); //$NON-NLS-1$
    }

    private static void printArgument(Printer printer, String title, Object value) {
        printer.printf("%s - %s", title, value); //$NON-NLS-1$
    }

    static Monitor createMonitor(@Nullable Path path) throws IOException {
        if (path == null) {
            // default monitoring
            return new LoggingMonitor(Constants.APPLICATION_NAME, LOG);
        }
        if (Files.exists(path)) {
            throw new IOException(MessageFormat.format(
                    "file already exists on the monitor output path: {0}",
                    path));
        }
        LOG.debug("creating monitor output: {}", path); //$NON-NLS-1$
        var parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return new CompositeMonitor(List.of(createMonitor(null), new JsonMonitor(path)));
    }

    static DumpProfile loadProfile(@Nonnull DumpProfileBundleLoader loader, @Nonnull Path profile)
            throws DiagnosticException {
        Objects.requireNonNull(loader);
        Objects.requireNonNull(profile);
        LOG.debug("inspecting profile: {}", profile);
        if (!Files.isRegularFile(profile) && profile.getNameCount() == 1) {
            LOG.debug("find from built-in profile: {}", profile);
            var bundle = loader.load();
            var profileName = profile.toString();
            var found = bundle.getProfile(profileName);
            LOG.debug("found built-in profile: {}", found); //$NON-NLS-1$
            return found;
        }
        LOG.debug("find profile from the file system: {}", profile);
        return loader.loadProfile(profile);
    }

    static List<DumpTarget> prepareDestination(
            @Nonnull DumpTargetSelector selector,
            @Nonnull Path destinationPath,
            @Nonnull List<String> tableNames) throws DiagnosticException {
        return prepareDestination(selector, destinationPath, tableNames, false);
    }

    static List<DumpTarget> prepareDestination(
            @Nonnull DumpTargetSelector selector,
            @Nonnull Path destinationPath,
            @Nonnull List<String> tableNames,
            boolean singleMode) throws DiagnosticException {
        Objects.requireNonNull(selector);
        Objects.requireNonNull(destinationPath);
        Objects.requireNonNull(tableNames);
        if (tableNames.isEmpty()) {
            throw new CliException(CliDiagnosticCode.INVALID_PARAMETER,
                    List.of("no table name or query text specified"));
        }
        if (singleMode && tableNames.size() != 1) {
            throw new CliException(CliDiagnosticCode.INVALID_PARAMETER,
                    List.of("single mode requires only one table name or query text"));
        }
        LOG.debug("inspecting destination: {}", destinationPath);
        Path destination;
        try {
            if (Files.isDirectory(destinationPath)) {
                if (Files.list(destinationPath).findAny().isPresent()) {
                    throw new CliException(CliDiagnosticCode.DESTINATION_EXISTS,
                            List.of(destinationPath));
                }
            } else {
                LOG.debug("create base destination directory: {}", destinationPath);
                Files.createDirectories(destinationPath);
            }
            destination = destinationPath.toAbsolutePath().toRealPath();
        } catch (IOException | IOError e) {
            throw new CliException(CliDiagnosticCode.DESTINATION_FAILURE,
                    List.of(destinationPath),
                    e);
        }

        LOG.debug("compute individual dump output directories");
        List<DumpTarget> targets;
        try {
            if (singleMode) {
                targets = List.of(selector.getTarget(destination, tableNames.get(0)));
            } else {
                targets = selector.getTargets(destination, tableNames);
            }
        } catch (IllegalArgumentException e) {
            throw new CliException(CliDiagnosticCode.INVALID_PARAMETER,
                    List.of(DiagnosticUtil.getMessage(e)),
                    e);
        }
        LOG.debug("dump targets: {}", targets);
        return targets;
    }
}
