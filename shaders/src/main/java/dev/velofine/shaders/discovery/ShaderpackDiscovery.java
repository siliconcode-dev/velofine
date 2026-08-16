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

package dev.velofine.shaders.discovery;

import dev.velofine.core.log.VelofineLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Scans {@code <gameDir>/shaderpacks/} for available packs - the same drop-in-a-folder,
 * select-by-name convention OptiFine/Iris use, not an in-game file picker (see CLAUDE.md's Phase 7
 * research notes on why {@code dev.velofine.core.gui} doesn't have one). A "pack" is either a
 * {@code .zip} file or a directory directly inside {@code shaderpacks/} that itself contains a
 * {@code shaders/} subfolder - loosely modeled on IrisShaders/Iris's
 * {@code ShaderpackDirectoryManager} (LGPL-3.0), reimplemented rather than ported since the real
 * logic here is a handful of lines of {@code java.nio.file} calls.
 */
public final class ShaderpackDiscovery {

    private final Path shaderpacksDirectory;

    public ShaderpackDiscovery(Path gameDir) {
        this.shaderpacksDirectory = gameDir.resolve("shaderpacks");
    }

    public Path getShaderpacksDirectory() {
        return shaderpacksDirectory;
    }

    /** Names of every valid pack found, sorted case-insensitively. Creates the folder if missing. */
    public List<String> enumerate() {
        try {
            Files.createDirectories(shaderpacksDirectory);
        } catch (IOException e) {
            VelofineLog.warn("Shaders", "Could not create shaderpacks directory: " + e);
            return List.of();
        }

        try (Stream<Path> entries = Files.list(shaderpacksDirectory)) {
            return entries.filter(ShaderpackDiscovery::isValidPack)
                    .map(path -> path.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            VelofineLog.warn("Shaders", "Could not list shaderpacks directory: " + e);
            return List.of();
        }
    }

    private static boolean isValidPack(Path path) {
        String name = path.getFileName().toString();
        if (name.startsWith(".")) {
            return false;
        }
        if (Files.isRegularFile(path)) {
            return name.toLowerCase(Locale.ROOT).endsWith(".zip");
        }
        return Files.isDirectory(path) && Files.exists(path.resolve("shaders"));
    }
}
