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
 * One settings row: label on the left, current value in a box on the right.
 *
 * <p>Activation is directional: clicking the left half steps backwards, the right half forwards.
 * That gives integer settings a usable control without a draggable slider - drag handling is the
 * one piece of widget behaviour vanilla would not have given us for free, and it is not worth
 * writing blind against a UI that cannot be launched from this machine.
 */
public abstract class OptionRow extends VelofineWidget {

    /** Width of the value box on the right-hand side of the row. */
    public static final int VALUE_WIDTH = 74;

    private final String label;
    private final String description;
    private final Applies applies;

    protected OptionRow(int x, int y, int width, String label, String description, Applies applies) {
        super(x, y, width, VelofineTheme.ROW_HEIGHT, Component.literal(label));
        this.label = label;
        this.description = description;
        this.applies = applies;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public Applies applies() {
        return applies;
    }

    /** Rendered inside the value box. */
    protected abstract Component valueText();

    /** @param direction {@code +1} to step forwards, {@code -1} backwards. */
    protected abstract void activate(int direction);

    /** True when the value is a deliberate user override rather than the default. */
    protected boolean valueIsOverride() {
        return false;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!active) {
            return;
        }
        activate(event.x() < getX() + getWidth() / 2.0 ? -1 : 1);
    }

    // Deliberately does NOT override mouseScrolled. Rows cover most of the panel, so consuming
    // scroll here would make the option list unscrollable wherever the cursor actually is - and
    // worse, would let a player change a setting while trying to scroll past it.

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
            float partialTick) {
        boolean highlight = isHoveredOrFocused() && active;

        extractor.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                highlight ? VelofineTheme.PANEL_RAISED : VelofineTheme.PANEL);
        if (highlight) {
            VelofineTheme.marker(extractor, getX(), getY(), getHeight());
        }

        int labelColor = active ? (highlight ? VelofineTheme.TEXT : VelofineTheme.TEXT_DIM)
                : VelofineTheme.TEXT_DISABLED;
        String shownLabel = applies == Applies.RESTART ? label + " *" : label;
        extractor.text(font(), shownLabel, getX() + VelofineTheme.PADDING + 3, textY(), labelColor);

        int boxX = getX() + getWidth() - VALUE_WIDTH - VelofineTheme.PADDING;
        int boxBorder;
        int valueColor;
        if (!active) {
            boxBorder = VelofineTheme.BORDER;
            valueColor = VelofineTheme.TEXT_DISABLED;
        } else if (valueIsOverride()) {
            boxBorder = VelofineTheme.ACCENT;
            valueColor = VelofineTheme.TEXT;
        } else {
            boxBorder = highlight ? VelofineTheme.BORDER_BRIGHT : VelofineTheme.BORDER;
            valueColor = VelofineTheme.TEXT;
        }

        VelofineTheme.panel(extractor, boxX, getY() + 2, VALUE_WIDTH, getHeight() - 4,
                VelofineTheme.PANEL, boxBorder);
        extractor.centeredText(font(), valueText(), boxX + VALUE_WIDTH / 2, textY(), valueColor);
    }
}
