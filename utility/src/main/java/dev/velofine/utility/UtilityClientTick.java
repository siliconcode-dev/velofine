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

package dev.velofine.utility;

import com.mojang.blaze3d.platform.InputConstants;
import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.utility.light.DynamicLightsState;
import dev.velofine.utility.rendering.AntiAliasingController;
import dev.velofine.utility.zoom.ZoomState;
import net.minecraft.client.Minecraft;

/**
 * Everything Utility does once per client tick, behind one entry point so its {@code Minecraft}
 * mixin stays a one-line delegation - the same shape as {@code OptimusClientTick}.
 *
 * <p>Each feature is individually try/catch-guarded so a bug in one (e.g. a future dynamic-lights
 * scan) can never take the client tick loop, or another feature, down with it.
 */
public final class UtilityClientTick {

    private static boolean zoomFailed;
    private static boolean aaFailed;
    private static boolean lightsFailed;

    private UtilityClientTick() {
    }

    public static void onTickEnd() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || !minecraft.isRunning()) {
            return;
        }

        if (!zoomFailed) {
            try {
                tickZoom(minecraft);
            } catch (Throwable t) {
                zoomFailed = true;
                VelofineLog.warn("Utility", "Zoom disabled after an error: " + t);
                t.printStackTrace();
            }
        }

        if (!aaFailed) {
            try {
                AntiAliasingController.onTick(minecraft);
            } catch (Throwable t) {
                aaFailed = true;
                VelofineLog.warn("Utility", "Anti-aliasing disabled after an error: " + t);
                t.printStackTrace();
            }
        }

        if (!lightsFailed) {
            try {
                DynamicLightsState.onTick(minecraft);
            } catch (Throwable t) {
                lightsFailed = true;
                VelofineLog.warn("Utility", "Dynamic lights disabled after an error: " + t);
                t.printStackTrace();
            }
        }
    }

    private static void tickZoom(Minecraft minecraft) {
        int key = ConfigManager.get().utility.zoom.key;
        boolean inWorldNoScreen = minecraft.gui != null && minecraft.gui.screen() == null;
        boolean down = ConfigManager.get().utility.zoom.enabled && key >= 0 && inWorldNoScreen
                && InputConstants.isKeyDown(minecraft.getWindow(), key);
        ZoomState.onTick(down);
    }
}
