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
import dev.velofine.core.gui.Applies;
import dev.velofine.core.gui.ConfigPage;
import dev.velofine.core.gui.CycleRow;
import dev.velofine.core.gui.OptionRow;
import dev.velofine.core.hardware.HardwareProfiles;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility's panel.
 *
 * <p>Utility ships no features until Phase 6, and the footnote says so rather than the page
 * pretending otherwise with rows that do nothing. What exists now is the part Phase 5 owes it:
 * the master toggle and the safe-defaults policy switch that Phase 6's features will consult.
 */
public final class UtilityPage extends ConfigPage {

    @Override
    public String title() {
        return "UTILITY";
    }

    @Override
    public boolean engineEnabled(VelofineConfig working) {
        return working.engines.utility;
    }

    @Override
    public List<OptionRow> buildRows(VelofineConfig working, RowCursor cursor) {
        List<OptionRow> rows = new ArrayList<>();

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Utility engine",
                "Master switch for the quality-of-life feature set (zoom, dynamic lights, "
                        + "connected textures, shaders). Nothing is wired to it yet.",
                Applies.RESTART,
                () -> working.engines.utility,
                value -> working.engines.utility = value,
                true));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Safe defaults on weak HW",
                "When Velofine detects old-Intel graphics, low memory or a rotational disk, "
                        + "Utility features start switched off or reduced rather than on. You can "
                        + "still turn any of them on yourself.",
                Applies.LIVE,
                () -> working.utility.safeDefaultsOnWeakHardware,
                value -> working.utility.safeDefaultsOnWeakHardware = value,
                true));

        return rows;
    }

    @Override
    public String footnote() {
        String mode = HardwareProfiles.isWeakHardware() ? "safe mode would apply here" : "full features would apply here";
        return "no Utility features ship until Phase 6  |  " + mode;
    }
}
