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

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * A single non-interactive line of plain text inside a page's row list - the Overview page's
 * hardware panel (item 10: detected GPU, recommended profile, estimated performance) uses this for
 * each line rather than a bespoke multi-line panel widget, keeping the same uniform row height every
 * other row uses (see {@link CategoryHeaderRow}'s javadoc for why that's deliberate).
 */
public final class InfoRow extends OptionRow {

    private final int color;

    public InfoRow(int x, int y, int width, String text) {
        this(x, y, width, text, VelofineTheme.TEXT);
    }

    public InfoRow(int x, int y, int width, String text, int color) {
        super(x, y, width, text, "", Applies.LIVE);
        this.color = color;
    }

    @Override
    protected Component valueText() {
        return Component.empty();
    }

    @Override
    protected void activate(int direction) {
        // Non-interactive.
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        // Non-interactive.
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
            float partialTick) {
        extractor.text(font(), label(), getX() + VelofineTheme.PADDING + 3,
                getY() + getHeight() / 2 - 4, color);
    }
}
