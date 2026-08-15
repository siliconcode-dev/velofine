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

package net.minecraft.client;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;

public class Minecraft {

    public Font font;
    public Gui gui;
    public Options options;
    public ClientLevel level;

    public static Minecraft getInstance() {
        throw new UnsupportedOperationException("stub");
    }

    public boolean isRunning() {
        throw new UnsupportedOperationException("stub");
    }

    public int getFps() {
        throw new UnsupportedOperationException("stub");
    }

    public Window getWindow() {
        throw new UnsupportedOperationException("stub");
    }

    /** {@code Gui.setScreen} plus a forced frame - what vanilla's own menu buttons use. */
    public void setScreenAndShow(Screen screen) {
        throw new UnsupportedOperationException("stub");
    }
}
