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
 * A non-interactive section divider inside a page's row list (item 5: "Optimus is already begging
 * for ENGINE/GOVERNOR/DIAGNOSTICS"). Deliberately kept the same {@link VelofineTheme#CONTENT_ROW_HEIGHT}
 * as every other row rather than a shorter custom height - {@code RowCursor}/{@code
 * VelofineConfigScreen}'s scroll/draw stepping all assume a uniform row height, and a header reads
 * fine with a little extra breathing room around it rather than needing that assumption threaded
 * through as a special case.
 *
 * <p>Excluded from hover-selection in {@code VelofineConfigScreen.drawRows} (a header has nothing
 * to show in the detail panel) and from click handling ({@link #onClick} is a no-op).
 */
public final class CategoryHeaderRow extends OptionRow {

    public CategoryHeaderRow(int x, int y, int width, String label) {
        super(x, y, width, label, "", Applies.LIVE);
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
        // Non-interactive - deliberately does not delegate to OptionRow's value-box click handling.
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
            float partialTick) {
        int ruleY = getY() + getHeight() - 3;
        extractor.text(font(), label().toUpperCase(java.util.Locale.ROOT),
                getX() + VelofineTheme.PADDING, getY() + getHeight() / 2 - 8, VelofineTheme.TEXT_DIM);
        VelofineTheme.horizontalRule(extractor, getX(), ruleY, getWidth(), VelofineTheme.BORDER);
    }
}
