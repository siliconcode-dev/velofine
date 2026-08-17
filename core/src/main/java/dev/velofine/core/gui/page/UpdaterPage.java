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

package dev.velofine.core.gui.page;

import dev.velofine.core.BuildInfo;
import dev.velofine.core.config.VelofineConfig;
import dev.velofine.core.gui.Applies;
import dev.velofine.core.gui.ConfigPage;
import dev.velofine.core.gui.CycleRow;
import dev.velofine.core.gui.InfoRow;
import dev.velofine.core.gui.NavigateRow;
import dev.velofine.core.gui.OptionRow;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.core.status.LiveStatus;
import dev.velofine.core.updater.UpdateOrchestrator;
import dev.velofine.core.updater.UpdatePhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 8's updater panel. No master engine toggle ({@link #engineEnabled} is always {@code true})
 * - unlike LegacySupport/Optimus/Utility, the updater installs no mixins and has no vanilla+
 * feature to disable; {@code Auto-check on launch} alone covers "don't check automatically".
 *
 * <p>The "Update &amp; Restart" row only appears once {@link UpdateOrchestrator#phase()} is
 * {@link UpdatePhase#VERIFIED} - neither {@code OptimusPage} nor {@code UtilityPage} conditionally
 * render rows today, so this is the first page to do so; kept to a single {@code if} rather than a
 * new abstraction, per the Phase 8 plan.
 *
 * <p>Confirming spawn succeeded before calling {@link Minecraft#stop()} (not
 * {@code System.exit()}) is deliberate: {@code stop()} is confirmed real via javap against the
 * real 26.2 jar - it flips the same {@code running} flag the main loop already checks every
 * iteration, so the game unwinds through its own shutdown path (world save, resource cleanup)
 * instead of a hard process kill.
 */
public final class UpdaterPage extends ConfigPage {

    @Override
    public String title() {
        return "UPDATES";
    }

    @Override
    public boolean engineEnabled(VelofineConfig working) {
        return true;
    }

    @Override
    public List<OptionRow> buildRows(VelofineConfig working, RowCursor cursor, Screen screen) {
        List<OptionRow> rows = new ArrayList<>();

        rows.add(new NavigateRow(cursor.x(), cursor.nextY(), cursor.width(),
                "This build",
                "The Velofine version currently running.",
                BuildInfo.displayString(),
                () -> {
                })
                .withDetail("The Velofine version currently running and the Minecraft version it targets."));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Auto-check on launch",
                "Checks for updates automatically when the game starts.",
                Applies.LIVE,
                () -> working.updater.autoCheckOnLaunch,
                value -> working.updater.autoCheckOnLaunch = value,
                true)
                .withDetail("Checks for a newer Velofine release once every ~24 hours when the game starts. "
                        + "Metadata only (which version exists) - never sends anything about you or "
                        + "your system."));

        rows.add(new NavigateRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Check for updates",
                "Looks for a newer Velofine release right now.",
                "CHECK >",
                UpdateOrchestrator::checkNow)
                .withDetail("Looks for a newer release matching this install's Minecraft version, and - if "
                        + "one exists - downloads it and checks its SHA-256 checksum and Ed25519 "
                        + "signature automatically. Nothing is ever installed without a further, "
                        + "separate confirmation below."));

        rows.add(new InfoRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Last successful check: " + formatLastCheck(working.updater.lastCheckedEpochMillis)));

        UpdatePhase phase = UpdateOrchestrator.phase();
        if (phase == UpdatePhase.VERIFIED) {
            rows.add(new NavigateRow(cursor.x(), cursor.nextY(), cursor.width(),
                    "Update & restart",
                    "Installs the update and restarts the game.",
                    "UPDATE & RESTART >",
                    UpdaterPage::applyAndExit)
                    .withDetail("Closes Minecraft and silently installs the verified update. You will need to "
                            + "launch Minecraft again afterward from your launcher."));
        }

        // item 12: never leave a raw exception as the only place to find out what actually went
        // wrong - this row only appears on failure, and its own short line stays plain-language;
        // the technical detail (LiveStatus.updaterDetail()) only shows up if this row is hovered.
        if (phase == UpdatePhase.FAILED) {
            rows.add(new NavigateRow(cursor.x(), cursor.nextY(), cursor.width(),
                    "Update error",
                    "Hover for the technical error message.",
                    "VIEW DETAILS >",
                    () -> {
                    })
                    .withDetail(LiveStatus.updaterDetail().isEmpty()
                            ? "No further detail was captured for this failure."
                            : LiveStatus.updaterDetail()));
        }

        return rows;
    }

    private static String formatLastCheck(long epochMillis) {
        if (epochMillis <= 0) {
            return "Never";
        }
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())
                .format(Instant.ofEpochMilli(epochMillis));
    }

    @Override
    public String footnote() {
        return LiveStatus.updater();
    }

    private static void applyAndExit() {
        try {
            UpdateOrchestrator.apply();
        } catch (Exception e) {
            VelofineLog.warn("updater", "Failed to launch the downloaded installer: " + e);
            return;
        }
        Minecraft.getInstance().stop();
    }
}
