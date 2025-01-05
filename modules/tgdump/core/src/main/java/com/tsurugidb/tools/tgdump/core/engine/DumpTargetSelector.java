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
package com.tsurugidb.tools.tgdump.core.engine;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.tsurugidb.tools.tgdump.core.model.DumpTarget;

/**
 * Computes {@link DumpTarget dump targets} for each commands.
 */
public interface DumpTargetSelector {

    /**
     * Default delimiter character before destination path sequence.
     */
    char DEFAULT_SEQUENCE_DELIMITER = '-';

    /**
     * Computes a {@link DumpTarget dump target} for the single command.
     * <p>
     * The resulting dump target will be just the {@code destinationDirectory} itself.
     * </p>
     * @param destinationDirectory the output directory
     * @param command the command
     * @return the dump target for the command
     * @throws IllegalArgumentException if the command is not valid
     */
    DumpTarget getTarget(@Nonnull Path destinationDirectory, @Nonnull String command);

    /**
     * Computes {@link DumpTarget dump targets} for each commands.
     * @param destinationDirectory the base destination directory,
     *      each dump target will be placed under it.
     * @param commands the command list 
     * @return the dump targets for the commands
     * @throws IllegalArgumentException if some commands are not valid
     */
    List<DumpTarget> getTargets(@Nonnull Path destinationDirectory, @Nonnull List<String> commands);

    /**
     * Removes conflict from the dump target destinations, by appending unique suffixes.
     * @param targets the dump target list
     * @param delimiter the delimiter character
     * @return the dump target list without duplicates
     * @throws IllegalArgumentException if any destination name includes the delimiter character
     */
    static List<DumpTarget> resolveConflicts(@Nonnull List<DumpTarget> targets, char delimiter) {
        Objects.requireNonNull(targets);
        // SELECT destination, 0 FROM GROUP BY destination HAVING count(*) >= 2
        var conflicts = targets.stream()
                .peek(it -> {
                    var name = it.getDestination().getFileName();
                    Objects.requireNonNull(name);
                    if (name.toString().indexOf(delimiter) >= 0) {
                        throw new IllegalArgumentException(MessageFormat.format(
                                "dump target destination must not include the delimiter character ({0}) : {1}",
                                delimiter,
                                it.getDestination()));
                    }
                })
                .collect(Collectors.groupingBy(DumpTarget::getDestination, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() >= 2)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new AtomicInteger()));

        // if conflicts are found, rewrite its destination by appending the delimiter and suffixes
        var results = new ArrayList<DumpTarget>(targets.size());
        for (var target : targets) {
            var destination = target.getDestination();
            var counter = conflicts.get(target.getDestination());
            if (counter == null) {
                // no counflicts
                results.add(target);
            } else {
                // has conflicts, append delimiter and unique index to its tail
                var index = counter.incrementAndGet();
                var suffixed = String.format("%s%s%d", destination.getFileName(), delimiter, index); //$NON-NLS-1$
                results.add(new DumpTarget(
                        target.getTargetType(),
                        target.getLabel(),
                        target.getTarget(),
                        destination.resolveSibling(suffixed)));
            }
        }
        return results;
    }
}