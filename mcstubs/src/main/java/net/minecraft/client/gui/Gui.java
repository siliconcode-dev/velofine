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

package net.minecraft.client.gui;

import net.minecraft.client.gui.screens.Screen;

/**
 * 26.2 moved current-screen ownership out of {@code Minecraft} and onto {@code Gui} - there is no
 * {@code Minecraft.screen} field or {@code getScreen()} in this version.
 */
public class Gui {

    public Screen screen() {
        throw new UnsupportedOperationException("stub");
    }

    public void setScreen(Screen screen) {
        throw new UnsupportedOperationException("stub");
    }
}
