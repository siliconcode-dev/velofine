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

import dev.velofine.core.config.Tri;
import dev.velofine.core.config.VelofineConfig;
import dev.velofine.core.gui.Applies;
import dev.velofine.core.gui.ConfigPage;
import dev.velofine.core.gui.CycleRow;
import dev.velofine.core.gui.OptionRow;
import dev.velofine.core.hardware.Fix;
import dev.velofine.core.hardware.HardwareProfile;
import dev.velofine.core.hardware.HardwareProfiles;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * LegacySupport's panel: the master toggle plus a three-way override per {@link Fix}.
 *
 * <p>Every fix defaults to AUTO, meaning "whatever hardware detection decided". Forcing one ON is
 * how a tester on the reference machines can check a specific hypothesis in isolation; forcing one
 * OFF is how they rule one out. That is exactly the loop Build_plan asks for on hardware nobody
 * here can reach.
 */
public final class LegacySupportPage extends ConfigPage {

    private static final List<Tri> TRI_VALUES = List.of(Tri.AUTO, Tri.ON, Tri.OFF);

    @Override
    public String title() {
        return "LEGACYSUPPORT";
    }

    @Override
    public boolean engineEnabled(VelofineConfig working) {
        return working.engines.legacySupport;
    }

    @Override
    public List<OptionRow> buildRows(VelofineConfig working, RowCursor cursor, Screen screen) {
        List<OptionRow> rows = new ArrayList<>();

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "LegacySupport engine",
                "Master switch. When off, none of LegacySupport's mixins are applied at all - the "
                        + "game runs exactly as unmodified vanilla in this respect.",
                Applies.RESTART,
                () -> working.engines.legacySupport,
                value -> working.engines.legacySupport = value,
                true));

        rows.add(fixRow(working, cursor, Fix.GL_COMPATIBILITY_PROFILE,
                "GL compatibility profile",
                "Requests an OpenGL 3.3 Compatibility context instead of vanilla's Core "
                        + "Forward-Compatible one. This is the main candidate fix for invisible "
                        + "portals, lava and water on Intel HD 4000 / HD 2500 hardware.",
                Applies.RESTART));

        rows.add(fixRow(working, cursor, Fix.SHADER_MIX_PATCH,
                "Shader mix() patch",
                "Wraps constant blend factors passed to GLSL mix() so a documented Intel driver "
                        + "constant-folding bug cannot produce a wrong result.",
                Applies.RESTART));

        rows.add(fixRow(working, cursor, Fix.IO_STALL_SMOOTHING,
                "IO stall smoothing",
                "Throttles how many dirty chunks are queued for eager saving each tick, so chunk "
                        + "saves compete less with chunk loads on a rotational disk.",
                Applies.LIVE));

        rows.add(fixRow(working, cursor, Fix.MEMORY_SAVING_DEFAULTS,
                "Memory-saving defaults",
                "Lowers the first-run defaults for render/simulation distance, entity distance, "
                        + "mipmaps and particles. Only affects a profile that has no options.txt "
                        + "yet - it never overwrites settings you already chose.",
                Applies.RESTART));

        return rows;
    }

    @Override
    public String footnote() {
        HardwareProfile profile = HardwareProfiles.get();
        String gpu = profile.gpu().adapterName() != null ? profile.gpu().adapterName() : "unknown GPU";
        String memory = profile.memory().isLowMemory() ? "low memory" : "memory OK";
        String disk = profile.disk().rotational() ? "HDD" : "SSD/unknown";
        return "detected: " + gpu + "  |  " + memory + "  |  " + disk;
    }

    private static OptionRow fixRow(VelofineConfig working, RowCursor cursor, Fix fix, String label,
            String description, Applies applies) {
        return new CycleRow<>(cursor.x(), cursor.nextY(), cursor.width(), label, description, applies,
                TRI_VALUES, Enum::name,
                () -> working.legacySupport.mode(fix),
                value -> working.legacySupport.setMode(fix, value),
                Tri.AUTO);
    }
}
