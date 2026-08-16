/*
 * This file is part of Velofine.
 *
 * Velofine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Velofine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Velofine. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2026 siliconcode-dev
 */

package dev.velofine.shaders.format.option;

import dev.velofine.core.log.VelofineLog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Scans every {@code .vsh}/{@code .fsh} directly under a pack's {@code shaders/} folder for
 * OptiFine-style annotated {@code #define} options - e.g. {@code #define SHADOW_QUALITY 1 //
 * Shadow quality [0 1 2 3]} (a selectable option) vs. plain {@code #define PI 3.14159} (not one -
 * {@link StringOption#create} returns {@code null} without a bracketed value list, matching
 * Iris's own convention this class's sibling types were adapted from).
 *
 * <p><b>Real scope cut, not silently wrong</b>: Iris discovers options across a full {@code
 * #include}-dependency graph, so an option declared once in a commonly-included file is found
 * everywhere it's referenced. This scans each file directly and independently instead - an option
 * that a pack only declares inside an {@code #include}d file (rather than in the gbuffers/
 * composite/final files themselves) will not be discovered. Revisit if a validation pack's real
 * tunable options turn out to live in included files.
 */
public final class OptionDiscovery {

    private static final Pattern DEFINE_LINE =
            Pattern.compile("^\\s*#define\\s+(\\w+)(?:\\s+(\\S+))?\\s*(?://\\s*(.*))?$");

    private OptionDiscovery() {
    }

    public static OptionSet discover(Path shadersDir) {
        OptionSet.Builder builder = OptionSet.builder();

        if (!Files.isDirectory(shadersDir)) {
            return builder.build();
        }

        try (Stream<Path> files = Files.walk(shadersDir)) {
            files.filter(OptionDiscovery::isShaderSource).forEach(file -> scanFile(file, builder));
        } catch (IOException e) {
            VelofineLog.warn("Shaders", "Failed to scan " + shadersDir + " for options: " + e);
        }

        return builder.build();
    }

    private static boolean isShaderSource(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".vsh") || name.endsWith(".fsh");
    }

    private static void scanFile(Path file, OptionSet.Builder builder) {
        String contents;
        try {
            contents = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return;
        }

        for (String line : contents.split("\\R")) {
            Matcher matcher = DEFINE_LINE.matcher(line);
            if (!matcher.matches()) {
                continue;
            }

            String name = matcher.group(1);
            String value = matcher.group(2);
            String comment = matcher.group(3);

            if (value == null) {
                // A bare flag: #define NAME // comment - a real boolean toggle, on by virtue of
                // being defined at all.
                builder.addBooleanOption(new BooleanOption(OptionType.DEFINE, name, comment, true));
                continue;
            }

            StringOption stringOption = StringOption.create(OptionType.DEFINE, name, comment, value);
            if (stringOption != null) {
                builder.addStringOption(stringOption);
            }
            // No bracketed allowed-value list in the comment: a plain numeric/text #define with
            // no user-facing option, matching Iris's own convention - not added to the set.
        }
    }
}
