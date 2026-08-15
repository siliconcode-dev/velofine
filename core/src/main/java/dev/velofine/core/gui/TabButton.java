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

import java.util.function.BooleanSupplier;

/** One entry in the left-hand engine rail. Selected state is drawn as a solid red marker block. */
public final class TabButton extends VelofineWidget {

    private final Runnable onSelect;
    private final BooleanSupplier selected;
    private final BooleanSupplier engineEnabled;

    public TabButton(int x, int y, int width, String label, BooleanSupplier selected,
            BooleanSupplier engineEnabled, Runnable onSelect) {
        super(x, y, width, VelofineTheme.ROW_HEIGHT, Component.literal(label));
        this.selected = selected;
        this.engineEnabled = engineEnabled;
        this.onSelect = onSelect;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onSelect.run();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
            float partialTick) {
        boolean isSelected = selected.getAsBoolean();
        boolean highlight = isSelected || isHoveredOrFocused();

        extractor.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                highlight ? VelofineTheme.PANEL_RAISED : VelofineTheme.PANEL);
        if (isSelected) {
            VelofineTheme.marker(extractor, getX(), getY(), getHeight());
        }

        // A disabled engine's tab stays reachable - that is the whole point, you need to get in
        // there to switch it back on - but reads dim so the state is obvious from the rail alone.
        int textColor;
        if (!engineEnabled.getAsBoolean()) {
            textColor = VelofineTheme.TEXT_DISABLED;
        } else {
            textColor = highlight ? VelofineTheme.TEXT : VelofineTheme.TEXT_DIM;
        }
        extractor.text(font(), getMessage(), getX() + VelofineTheme.PADDING + 3, textY(), textColor);
    }
}
