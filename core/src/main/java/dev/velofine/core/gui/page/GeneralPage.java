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
import dev.velofine.core.gui.ConfigPage;
import dev.velofine.core.gui.KeybindRow;
import dev.velofine.core.gui.OptionRow;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/** Settings that belong to Velofine itself rather than to any one engine. */
public final class GeneralPage extends ConfigPage {

    @Override
    public String title() {
        return "GENERAL";
    }

    @Override
    public boolean engineEnabled(VelofineConfig working) {
        return true;
    }

    @Override
    public List<OptionRow> buildRows(VelofineConfig working, RowCursor cursor, Screen screen) {
        List<OptionRow> rows = new ArrayList<>();

        rows.add(new KeybindRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Open settings key",
                "Opens this screen from in-game. Click, then press a key to bind it, or press "
                        + "Escape to leave it unbound. This bind is Velofine's own and does not "
                        + "appear in Minecraft's Controls list.",
                () -> working.ui.openConfigKey,
                value -> working.ui.openConfigKey = value));

        return rows;
    }

    // No footnote here on purpose: the only candidate was ConfigManager.configPath(), and that's
    // a full local filesystem path (including the Windows username) - not something to surface in
    // a screen players will screenshot.
}
