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

package dev.velofine.core.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.velofine.core.config.ConfigManager;
import net.minecraft.client.Minecraft;

/**
 * Edge-detects Velofine's own "open settings" key once per client tick.
 *
 * <p>The rising-edge state lives in this ordinary class rather than in a {@code @Unique} static
 * field on the mixin, so the mixin stays a one-line delegation and no state has to be merged into
 * {@code Minecraft} itself.
 *
 * <p>Polling {@code InputConstants.isKeyDown} instead of registering a {@code KeyMapping} is a
 * deliberate trade-off, documented on {@link KeybindRow}: it costs an appearance in vanilla's
 * Controls list and buys not having to inject into the synthetic array factory that builds
 * {@code Options.keyMappings}.
 */
public final class ConfigKeyBinding {

    private static boolean wasDown;

    private ConfigKeyBinding() {
    }

    public static void poll(Minecraft minecraft) {
        int key = ConfigManager.get().ui.openConfigKey;
        if (key < 0) {
            wasDown = false;
            return;
        }

        boolean down = InputConstants.isKeyDown(minecraft.getWindow(), key);
        // Only fire while actually in the world with no screen open, so the bind can never steal
        // a keystroke from a text field or fight with another screen for control.
        if (down && !wasDown && minecraft.gui != null && minecraft.gui.screen() == null) {
            minecraft.setScreenAndShow(new VelofineConfigScreen(null));
        }
        wasDown = down;
    }
}
