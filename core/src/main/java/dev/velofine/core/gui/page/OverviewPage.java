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

import dev.velofine.core.config.VelofineConfig;
import dev.velofine.core.gui.CategoryHeaderRow;
import dev.velofine.core.gui.ConfigPage;
import dev.velofine.core.gui.InfoRow;
import dev.velofine.core.gui.KeybindRow;
import dev.velofine.core.gui.OptionRow;
import dev.velofine.core.gui.VelofineTheme;
import dev.velofine.core.hardware.HardwareProfile;
import dev.velofine.core.hardware.HardwareProfiles;
import dev.velofine.core.hardware.PerformanceEstimate;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * v1.5 settings-UI overhaul (item 10): renamed from {@code GeneralPage} and given an identity - a
 * hardware profile panel (detected GPU, a recommended profile, an estimated-performance bar) built
 * entirely from data {@link HardwareProfiles} already detects and caches (same source
 * {@link LegacySupportPage}'s footnote and {@link UtilityPage}'s footnote already read from), plus
 * Velofine's own settings that don't belong to any one engine.
 */
public final class OverviewPage extends ConfigPage {

    @Override
    public String title() {
        return "OVERVIEW";
    }

    @Override
    public boolean engineEnabled(VelofineConfig working) {
        return true;
    }

    @Override
    public List<OptionRow> buildRows(VelofineConfig working, RowCursor cursor, Screen screen) {
        List<OptionRow> rows = new ArrayList<>();
        HardwareProfile profile = HardwareProfiles.get();

        rows.add(new CategoryHeaderRow(cursor.x(), cursor.nextY(), cursor.width(), "Profile"));

        String gpuName = profile.gpu().adapterName() != null ? profile.gpu().adapterName() : "unknown GPU";
        rows.add(new InfoRow(cursor.x(), cursor.nextY(), cursor.width(), "Detected GPU: " + gpuName));

        String recommendedProfile = HardwareProfiles.isWeakHardware() ? "SAFE" : "FULL";
        int profileColor = HardwareProfiles.isWeakHardware() ? VelofineTheme.WARNING : VelofineTheme.SUCCESS;
        rows.add(new InfoRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Recommended profile: " + recommendedProfile, profileColor));

        rows.add(new InfoRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Estimated performance: " + PerformanceEstimate.label(profile) + " (rough estimate, not a benchmark)"));

        rows.add(new CategoryHeaderRow(cursor.x(), cursor.nextY(), cursor.width(), "General"));

        rows.add(new KeybindRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Open settings key",
                "Click, then press a key to open this menu from in-game.",
                () -> working.ui.openConfigKey,
                value -> working.ui.openConfigKey = value)
                .withDetail("Opens this screen from in-game. Click, then press a key to bind it, or press "
                        + "Escape to leave it unbound. This bind is Velofine's own and does not "
                        + "appear in Minecraft's Controls list."));

        return rows;
    }

    // No footnote here on purpose: the only candidate was ConfigManager.configPath(), and that's
    // a full local filesystem path (including the Windows username) - not something to surface in
    // a screen players will screenshot.
}
