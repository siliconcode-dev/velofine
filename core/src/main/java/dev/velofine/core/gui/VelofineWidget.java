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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * Base for every hand-drawn Velofine widget.
 *
 * <p>Extending vanilla's {@code AbstractWidget} rather than reimplementing a widget system is
 * deliberate: it has exactly two abstract methods (confirmed via javap against the real 26.2 jar),
 * so Velofine inherits all of vanilla's geometry, hover tracking, focus and tab-order handling and
 * only replaces the <em>drawing</em>. That keeps keyboard navigation and the narrator working in a
 * fully custom-looking UI, which a from-scratch widget tree would have quietly dropped.
 */
public abstract class VelofineWidget extends net.minecraft.client.gui.components.AbstractWidget {

    protected VelofineWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }

    /** Hit test against this widget's own bounds, independent of vanilla's hover bookkeeping. */
    public boolean containsPoint(int x, int y) {
        return x >= getX() && x < getX() + getWidth() && y >= getY() && y < getY() + getHeight();
    }

    /** Vertically centred text baseline for a single line inside this widget. */
    protected int textY() {
        return getY() + (getHeight() - 8) / 2;
    }

    /**
     * Resolved on demand rather than cached in a field: widgets are rebuilt on every screen
     * resize, and {@code Minecraft.getInstance().font} is a stable final field, so there is
     * nothing to gain from holding a reference and something to lose if a widget ever outlives
     * the screen that made it.
     */
    protected static Font font() {
        return Minecraft.getInstance().font;
    }
}
