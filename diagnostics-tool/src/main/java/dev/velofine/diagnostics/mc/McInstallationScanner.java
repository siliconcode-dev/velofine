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

package dev.velofine.diagnostics.mc;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Scans a Minecraft installation directory's real {@code versions/<id>/<id>.jar} layout - the same
 * standard-format layout Velofine's own {@code ProfileInstaller} already reads/writes (Mojang
 * launcher and TLauncher-family launchers both use it).
 */
public final class McInstallationScanner {

    /** Preferred version for this v1.5 release - selected first in the picker UI when present. */
    public static final String PREFERRED_VERSION_ID = "26.2";

    private McInstallationScanner() {
    }

    /** True if {@code mcDir} looks like a real Minecraft installation directory. */
    public static boolean isValidInstallation(Path mcDir) {
        return mcDir != null && Files.isDirectory(mcDir) && Files.isDirectory(mcDir.resolve("versions"));
    }

    /**
     * Lists every version under {@code <mcDir>/versions/} that has a matching {@code <id>.jar},
     * sorted with {@link #PREFERRED_VERSION_ID} first, then reverse-alphabetically (newest-looking
     * versions first as a reasonable heuristic for anything else installed).
     */
    public static List<McVersionEntry> scan(Path mcDir) throws IOException {
        List<McVersionEntry> result = new ArrayList<>();
        Path versionsDir = mcDir.resolve("versions");
        if (!Files.isDirectory(versionsDir)) {
            return result;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(versionsDir)) {
            for (Path versionDir : stream) {
                if (!Files.isDirectory(versionDir)) {
                    continue;
                }
                String versionId = versionDir.getFileName().toString();
                Path jar = versionDir.resolve(versionId + ".jar");
                if (Files.isRegularFile(jar)) {
                    result.add(new McVersionEntry(versionId, jar));
                }
            }
        }

        result.sort(Comparator
                .comparing((McVersionEntry e) -> !e.versionId().equals(PREFERRED_VERSION_ID))
                .thenComparing(McVersionEntry::versionId, Comparator.reverseOrder()));
        return result;
    }
}
