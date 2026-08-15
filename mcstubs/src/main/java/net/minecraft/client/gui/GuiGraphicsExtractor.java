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

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * 26.2 renders GUIs through a render-state <em>extraction</em> pass rather than the older
 * {@code render(GuiGraphics, ...)} immediate-mode calls, so every draw Velofine does goes through
 * this type. Only the primitives the brutalist UI actually uses are declared.
 */
public class GuiGraphicsExtractor {

    public int guiWidth() {
        throw new UnsupportedOperationException("stub");
    }

    public int guiHeight() {
        throw new UnsupportedOperationException("stub");
    }

    public void fill(int x1, int y1, int x2, int y2, int color) {
        throw new UnsupportedOperationException("stub");
    }

    public void enableScissor(int x1, int y1, int x2, int y2) {
        throw new UnsupportedOperationException("stub");
    }

    public void disableScissor() {
        throw new UnsupportedOperationException("stub");
    }

    public void text(Font font, String text, int x, int y, int color) {
        throw new UnsupportedOperationException("stub");
    }

    public void text(Font font, Component text, int x, int y, int color) {
        throw new UnsupportedOperationException("stub");
    }

    public void centeredText(Font font, Component text, int x, int y, int color) {
        throw new UnsupportedOperationException("stub");
    }
}
