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

package dev.velofine.diagnostics.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Remembers the last-used Minecraft directory across runs so repeat testing (Phase 4's
 * fix-iteration loop especially) doesn't require re-picking the folder every time. Plain text, not
 * JSON - trivially small, no need for Gson here. Purely local, no network - consistent with the
 * tool's no-telemetry requirement.
 */
public final class LastDirStore {

    private LastDirStore() {
    }

    private static Path storeFile() {
        String localAppData = System.getenv("LOCALAPPDATA");
        Path base = localAppData != null ? Paths.get(localAppData) : Paths.get(System.getProperty("user.home"));
        return base.resolve("VelofineDiagnostic").resolve("lastMcDir.txt");
    }

    public static Path load() {
        try {
            Path file = storeFile();
            if (!Files.isRegularFile(file)) {
                return null;
            }
            String text = Files.readString(file, StandardCharsets.UTF_8).strip();
            return text.isEmpty() ? null : Paths.get(text);
        } catch (Exception e) {
            return null;
        }
    }

    public static void save(Path mcDir) {
        try {
            Path file = storeFile();
            Files.createDirectories(file.getParent());
            Files.writeString(file, mcDir.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Best-effort convenience only - failing to remember the directory must never block the tool.
        }
    }
}
