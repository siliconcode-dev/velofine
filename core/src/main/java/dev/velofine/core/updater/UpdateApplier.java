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

package dev.velofine.core.updater;

import dev.velofine.core.config.ConfigManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Spawns a <em>verified</em> {@code Velofine-Setup-<version>.exe} silently, reusing Inno Setup's
 * own already-correct file layout and {@code --install-profile} refresh logic instead of
 * hand-rolling file replacement (Phase 8's explicit design decision - see the Phase 8 plan).
 *
 * <p>Deliberately references no {@code core.gui}/Minecraft type, same as the rest of this package
 * - callers decide when it is safe to call (only ever from a user-triggered GUI action, immediately
 * before {@code Minecraft.getInstance().stop()}; see {@code UpdaterPage}). Ordering matters: this
 * must only be invoked <em>after</em> the game JVM has already committed to stopping, since the
 * silently-spawned installer's {@code [Run]} step re-invokes {@code --install-profile}, which
 * rewrites the {@code .minecraft\libraries\dev\velofine\velofine-launcher\<version>\...jar} tree -
 * safe once the just-exiting JVM has released whatever version-specific jar it had open, but not
 * guaranteed safe while it is still running.
 *
 * <p>No {@code AppMutex}/running-instance guard exists in {@code installer.iss} - this ordering
 * assumption is the only thing standing in for one (see the Phase 8 plan's open trade-offs list).
 */
final class UpdateApplier {

    private UpdateApplier() {
    }

    /**
     * Launches the installer as an independent OS process (not a child tied to this JVM's
     * lifetime) and writes a marker so the next launch can confirm success. Does not itself stop
     * the game - the caller does that immediately afterward.
     */
    static void spawnInstaller(Path verifiedInstallerExe) throws IOException {
        writeAppliedMarker(verifiedInstallerExe.getFileName().toString());
        new ProcessBuilder(
                verifiedInstallerExe.toAbsolutePath().toString(),
                "/VERYSILENT",
                "/SUPPRESSMSGBOXES",
                "/NORESTART")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
    }

    private static void writeAppliedMarker(String installerFilename) throws IOException {
        Path configPath = ConfigManager.configPath();
        if (configPath == null) {
            return;
        }
        Path marker = configPath.getParent().resolve("update-applied.marker");
        Files.writeString(marker, installerFilename, StandardCharsets.UTF_8);
    }

    /**
     * Called once, early in the next launch (after the silent re-install has had a chance to run)
     * to surface a one-time "update applied successfully" confirmation and then clear the marker
     * so it is not shown again.
     */
    static String consumeAppliedMarkerIfPresent() {
        Path configPath = ConfigManager.configPath();
        if (configPath == null) {
            return null;
        }
        Path marker = configPath.getParent().resolve("update-applied.marker");
        if (!Files.isRegularFile(marker)) {
            return null;
        }
        try {
            String content = Files.readString(marker, StandardCharsets.UTF_8);
            Files.deleteIfExists(marker);
            return content;
        } catch (IOException e) {
            return null;
        }
    }
}
