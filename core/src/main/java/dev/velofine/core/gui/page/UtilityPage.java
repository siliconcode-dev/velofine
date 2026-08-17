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
import dev.velofine.core.gui.CategoryHeaderRow;
import dev.velofine.core.gui.ConfigPage;
import dev.velofine.core.gui.CycleRow;
import dev.velofine.core.gui.IntRow;
import dev.velofine.core.gui.KeybindRow;
import dev.velofine.core.gui.NavigateRow;
import dev.velofine.core.gui.OptionRow;
import dev.velofine.core.gui.ShaderOptionsScreen;
import dev.velofine.core.hardware.HardwareProfiles;
import dev.velofine.core.shader.ShaderPackBrowserRegistry;
import dev.velofine.core.status.LiveStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

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
    public List<OptionRow> buildRows(VelofineConfig working, RowCursor cursor, Screen screen) {
        List<OptionRow> rows = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        rows.add(new CategoryHeaderRow(cursor.x(), cursor.nextY(), cursor.width(), "Engine"));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Utility engine",
                "Turns all Utility quality-of-life features on or off.",
                Applies.RESTART,
                () -> working.engines.utility,
                value -> working.engines.utility = value,
                true)
                .withDetail("Master switch for the quality-of-life feature set: zoom, fog/AA control, "
                        + "dynamic lights, connected textures."));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Safe defaults on weak HW",
                "Starts features off or reduced automatically on weak hardware.",
                Applies.LIVE,
                () -> working.utility.safeDefaultsOnWeakHardware,
                value -> working.utility.safeDefaultsOnWeakHardware = value,
                true)
                .withDetail("When Velofine detects old-Intel graphics, low memory or a rotational disk, "
                        + "Utility features start switched off or reduced rather than on. You can "
                        + "still turn any of them on yourself.")
                .withRecommended(() -> working.utility.safeDefaultsOnWeakHardware && HardwareProfiles.isWeakHardware()));

        rows.add(new CategoryHeaderRow(cursor.x(), cursor.nextY(), cursor.width(), "Zoom"));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Zoom",
                "Hold the zoom key to zoom in smoothly.",
                Applies.LIVE,
                () -> working.utility.zoom.enabled,
                value -> working.utility.zoom.enabled = value,
                true)
                .withDetail("Smooth, scroll-adjustable zoom while the zoom key is held. Overrides the FOV "
                        + "calculation directly, so it works well past vanilla's own 30-110 FOV slider."));

        rows.add(new KeybindRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Zoom key",
                "Key held down to zoom in.",
                () -> working.utility.zoom.key,
                value -> working.utility.zoom.key = value)
                .withDetail("Hold to zoom. Unbound by default - Velofine will not steal a key you already use."));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Max zoom",
                "How far the zoom key zooms in.",
                Applies.LIVE,
                2, 16, 1, "x",
                () -> (int) working.utility.zoom.maxZoomFactor,
                value -> working.utility.zoom.maxZoomFactor = value,
                4,
                true)
                .withDetail("How far the zoom key zooms in, as a multiple of your normal FOV. Loops back "
                        + "around past the top/bottom rather than getting stuck."));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Zoom scroll-adjust",
                "Scroll while zooming to fine-tune the zoom level.",
                Applies.LIVE,
                () -> working.utility.zoom.scrollAdjustable,
                value -> working.utility.zoom.scrollAdjustable = value,
                true)
                .withDetail("While holding the zoom key, scroll to fine-tune the current zoom level between "
                        + "1x and the maximum above."));

        rows.add(new CategoryHeaderRow(cursor.x(), cursor.nextY(), cursor.width(), "Lighting & Visuals"));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Dynamic lights",
                "Held light sources light up the area around you.",
                Applies.LIVE,
                () -> working.utility.dynamicLights.enabled,
                value -> working.utility.dynamicLights.enabled = value,
                true)
                .withDetail("Holding a torch, lantern, glowstone, jack-o'-lantern or lava bucket lights the "
                        + "area around you, without needing to place it down. Purely a rendering "
                        + "effect - never written to the world."));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Fog",
                "Turns off distance fog.",
                Applies.LIVE,
                () -> working.utility.rendering.fogEnabled,
                value -> working.utility.rendering.fogEnabled = value,
                true)
                .withDetail("Turning this off pushes the fog start/end distance far past view range, so the "
                        + "usual distance haze stops kicking in. Weather/underwater/lava fog colour "
                        + "is untouched."));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Anti-aliasing (FXAA)",
                "Smooths jagged edges. Small GPU cost.",
                Applies.LIVE,
                () -> working.utility.rendering.antiAliasing,
                value -> working.utility.rendering.antiAliasing = value,
                false)
                .withDetail("A lightweight edge-smoothing post-process pass. Off by default - real per-frame "
                        + "GPU cost, and shares vanilla's one post-effect slot with things like "
                        + "portal blur, so it steps aside while one of those is active."));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Variable render distance",
                "Limits render distance vertically, separate from normal distance.",
                Applies.LIVE,
                () -> working.utility.renderDistance.enabled,
                value -> working.utility.renderDistance.enabled = value,
                false)
                .withDetail("Caps how many chunk sections above/below you get rendered, separately from your "
                        + "normal (horizontal) render distance below. Off is bit-identical to vanilla."));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Vertical distance",
                "How far chunks render above and below you.",
                Applies.LIVE,
                1, 32, 1, "",
                () -> working.utility.renderDistance.verticalDistance == 0 ? 8 : working.utility.renderDistance.verticalDistance,
                value -> working.utility.renderDistance.verticalDistance = value,
                8)
                .withDetail("How many chunk sections above/below you stay visible. Ignored while the row "
                        + "above is off."));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Mipmap levels",
                "Reduces shimmer on distant textures.",
                Applies.LIVE,
                0, 4, 1, "",
                () -> mc.options.mipmapLevels().get(),
                value -> mc.options.mipmapLevels().set(value),
                4)
                .withDetail("Vanilla's own setting, exposed here too. Higher reduces shimmer on distant "
                        + "textures at a small memory cost."));

        rows.add(new IntRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Anisotropic filtering",
                "Sharpens textures seen at a shallow angle.",
                Applies.LIVE,
                1, 3, 1, "",
                () -> mc.options.maxAnisotropyBit().get(),
                value -> mc.options.maxAnisotropyBit().set(value),
                2)
                .withDetail("Vanilla's own setting, exposed here too. Sharpens textures viewed at a shallow "
                        + "angle, e.g. floors/paths in the distance."));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "FPS overlay",
                "Shows live FPS and frame-time stats below.",
                Applies.LIVE,
                () -> working.utility.fpsOverlay.enabled,
                value -> working.utility.fpsOverlay.enabled = value,
                false)
                .withDetail("Samples frame time every frame and shows a live avg/1% low/0.1% low/frame-time "
                        + "readout in the footnote below - the same richer-than-F3 numbers modern "
                        + "benchmark tools track, not just an instantaneous counter."));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Vsync",
                "Syncs frame rate to your monitor.",
                Applies.LIVE,
                () -> mc.options.enableVsync().get(),
                value -> mc.options.enableVsync().set(value),
                true)
                .withDetail("Vanilla's own setting, exposed here too."));

        rows.add(new CategoryHeaderRow(cursor.x(), cursor.nextY(), cursor.width(), "Shaders"));

        rows.add(CycleRow.ofBoolean(cursor.x(), cursor.nextY(), cursor.width(),
                "Shaders",
                "Turns on shader pack support.",
                Applies.RESTART,
                () -> working.utility.shader.enabled,
                value -> working.utility.shader.enabled = value,
                false)
                .withDetail("OptiFine/Iris-format shader pack support. v1 targets vanilla 26.2's own real GLSL "
                        + "attribute/uniform names directly, not OptiFine's traditional gl_Vertex-style "
                        + "convention - a real pack authored for OptiFine/Iris will likely need porting."));

        List<String> packNames = new ArrayList<>();
        packNames.add("NONE");
        packNames.addAll(ShaderPackBrowserRegistry.listPackNames());
        rows.add(new CycleRow<>(cursor.x(), cursor.nextY(), cursor.width(),
                "Shader pack",
                "Pack loaded from your shaderpacks folder.",
                Applies.RESTART,
                packNames,
                value -> value,
                () -> working.utility.shader.selectedPackName == null ? "NONE" : working.utility.shader.selectedPackName,
                value -> working.utility.shader.selectedPackName = "NONE".equals(value) ? null : value,
                "NONE")
                .withDetail("Selected from shaderpacks/ (drop a pack folder or .zip in there, then reopen this "
                        + "menu to see it listed). Ignored while Shaders above is off."));

        rows.add(new NavigateRow(cursor.x(), cursor.nextY(), cursor.width(),
                "Shader pack options",
                "Open the active pack's own settings.",
                "OPEN >",
                () -> mc.setScreenAndShow(new ShaderOptionsScreen(screen)))
                .withDetail("The active pack's own tunable #define options, if it declares any - discovered by "
                        + "scanning its GLSL source. Editing one reloads the pack immediately."));

        return rows;
    }

    @Override
    public String footnote() {
        String mode = HardwareProfiles.isWeakHardware() ? "safe mode applies here" : "full features apply here";
        return LiveStatus.fps() + "  |  " + mode;
    }
}
