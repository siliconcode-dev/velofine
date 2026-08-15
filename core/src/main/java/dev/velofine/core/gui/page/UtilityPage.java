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
import dev.velofine.core.gui.IntRow;
import dev.velofine.core.gui.KeybindRow;
import dev.velofine.core.gui.OptionRow;
import dev.velofine.core.hardware.HardwareProfiles;
import dev.velofine.core.status.LiveStatus;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility's panel.
 *
 * <p>Most rows here bind to a {@code working} copy of {@link VelofineConfig} like every other
 * page, staged behind UNDO/APPLY. Three rows (Vsync, mipmap, anisotropic filtering) are the
 * exception: those are already real, live vanilla {@code Options} fields (confirmed via javap -
 * {@code Options.enableVsync()}/{@code mipmapLevels()}/{@code maxAnisotropyBit()}), so they bind
 * straight to {@code Minecraft.getInstance().options} and take effect - and persist to vanilla's
 * own {@code options.txt} - immediately on click, exactly like vanilla's own Video Settings screen.
 * That is a deliberate difference from this page's other rows, not an oversight.
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
        Minecraft mc = Minecraft.getInstance();

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Utility engine",
                "Master switch for the quality-of-life feature set: zoom, fog/AA control, "
                        + "dynamic lights, connected textures.",
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

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Zoom",
                "Smooth, scroll-adjustable zoom while the zoom key is held. Overrides the FOV "
                        + "calculation directly, so it works well past vanilla's own 30-110 FOV slider.",
                Applies.LIVE,
                () -> working.utility.zoom.enabled,
                value -> working.utility.zoom.enabled = value,
                true));

        rows.add(new KeybindRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Zoom key",
                "Hold to zoom. Unbound by default - Velofine will not steal a key you already use.",
                () -> working.utility.zoom.key,
                value -> working.utility.zoom.key = value));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Max zoom",
                "How far the zoom key zooms in, as a multiple of your normal FOV. Loops back "
                        + "around past the top/bottom rather than getting stuck.",
                Applies.LIVE,
                2, 16, 1, "x",
                () -> (int) working.utility.zoom.maxZoomFactor,
                value -> working.utility.zoom.maxZoomFactor = value,
                4,
                true));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Zoom scroll-adjust",
                "While holding the zoom key, scroll to fine-tune the current zoom level between "
                        + "1x and the maximum above.",
                Applies.LIVE,
                () -> working.utility.zoom.scrollAdjustable,
                value -> working.utility.zoom.scrollAdjustable = value,
                true));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Dynamic lights",
                "Holding a torch, lantern, glowstone, jack-o'-lantern or lava bucket lights the "
                        + "area around you, without needing to place it down. Purely a rendering "
                        + "effect - never written to the world.",
                Applies.LIVE,
                () -> working.utility.dynamicLights.enabled,
                value -> working.utility.dynamicLights.enabled = value,
                true));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Fog",
                "Turning this off pushes the fog start/end distance far past view range, so the "
                        + "usual distance haze stops kicking in. Weather/underwater/lava fog colour "
                        + "is untouched.",
                Applies.LIVE,
                () -> working.utility.rendering.fogEnabled,
                value -> working.utility.rendering.fogEnabled = value,
                true));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Anti-aliasing (FXAA)",
                "A lightweight edge-smoothing post-process pass. Off by default - real per-frame "
                        + "GPU cost, and shares vanilla's one post-effect slot with things like "
                        + "portal blur, so it steps aside while one of those is active.",
                Applies.LIVE,
                () -> working.utility.rendering.antiAliasing,
                value -> working.utility.rendering.antiAliasing = value,
                false));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Variable render distance",
                "Caps how many chunk sections above/below you get rendered, separately from your "
                        + "normal (horizontal) render distance below. Off is bit-identical to vanilla.",
                Applies.LIVE,
                () -> working.utility.renderDistance.enabled,
                value -> working.utility.renderDistance.enabled = value,
                false));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Vertical distance",
                "How many chunk sections above/below you stay visible. Ignored while the row "
                        + "above is off.",
                Applies.LIVE,
                1, 32, 1, "",
                () -> working.utility.renderDistance.verticalDistance == 0 ? 8 : working.utility.renderDistance.verticalDistance,
                value -> working.utility.renderDistance.verticalDistance = value,
                8));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Mipmap levels",
                "Vanilla's own setting, exposed here too. Higher reduces shimmer on distant "
                        + "textures at a small memory cost.",
                Applies.LIVE,
                0, 4, 1, "",
                () -> mc.options.mipmapLevels().get(),
                value -> mc.options.mipmapLevels().set(value),
                4));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Anisotropic filtering",
                "Vanilla's own setting, exposed here too. Sharpens textures viewed at a shallow "
                        + "angle, e.g. floors/paths in the distance.",
                Applies.LIVE,
                1, 3, 1, "",
                () -> mc.options.maxAnisotropyBit().get(),
                value -> mc.options.maxAnisotropyBit().set(value),
                2));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "FPS overlay",
                "Samples frame time every frame and shows a live avg/1% low/0.1% low/frame-time "
                        + "readout in the footnote below - the same richer-than-F3 numbers modern "
                        + "benchmark tools track, not just an instantaneous counter.",
                Applies.LIVE,
                () -> working.utility.fpsOverlay.enabled,
                value -> working.utility.fpsOverlay.enabled = value,
                false));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Vsync",
                "Vanilla's own setting, exposed here too.",
                Applies.LIVE,
                () -> mc.options.enableVsync().get(),
                value -> mc.options.enableVsync().set(value),
                true));

        return rows;
    }

    @Override
    public String footnote() {
        String mode = HardwareProfiles.isWeakHardware() ? "safe mode applies here" : "full features apply here";
        return LiveStatus.fps() + "  |  " + mode;
    }
}
