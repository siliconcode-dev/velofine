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

/** A plain rectangular command button: {@code UNDO}, {@code APPLY}, {@code CLOSE}, {@code VELOFINE}. */
public final class ActionButton extends VelofineWidget {

    /** Solid grey/black, used for secondary actions like {@code UNDO}/{@code CLOSE}. */
    public static final int STYLE_NORMAL = 0;
    /** Solid red fill, used for the one primary action on a screen (e.g. {@code APPLY}). */
    public static final int STYLE_PRIMARY = 1;
    /**
     * Black fill with an always-on red border, used where the button sits inside vanilla UI
     * (the pause menu) and needs to read as Velofine's without competing with vanilla's own
     * primary action on that screen.
     */
    public static final int STYLE_ACCENT_OUTLINE = 2;

    private final Runnable onPress;
    private final int style;

    public ActionButton(int x, int y, int width, int height, String label, int style, Runnable onPress) {
        super(x, y, width, height, Component.literal(label));
        this.style = style;
        this.onPress = onPress;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (active) {
            onPress.run();
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY,
            float partialTick) {
        boolean highlight = isHoveredOrFocused() && active;
        int fill;
        int borderColor;
        int textColor;
        if (!active) {
            fill = VelofineTheme.PANEL;
            borderColor = VelofineTheme.BORDER;
            textColor = VelofineTheme.TEXT_DISABLED;
        } else if (style == STYLE_PRIMARY) {
            fill = highlight ? VelofineTheme.ACCENT : VelofineTheme.ACCENT_DIM;
            borderColor = VelofineTheme.ACCENT;
            textColor = VelofineTheme.TEXT;
        } else if (style == STYLE_ACCENT_OUTLINE) {
            fill = highlight ? VelofineTheme.PANEL_RAISED : VelofineTheme.PANEL;
            borderColor = VelofineTheme.ACCENT;
            textColor = VelofineTheme.TEXT;
        } else {
            fill = highlight ? VelofineTheme.PANEL_RAISED : VelofineTheme.PANEL;
            borderColor = highlight ? VelofineTheme.BORDER_BRIGHT : VelofineTheme.BORDER;
            textColor = highlight ? VelofineTheme.TEXT : VelofineTheme.TEXT_DIM;
        }

        VelofineTheme.panel(extractor, getX(), getY(), getWidth(), getHeight(), fill, borderColor);
        extractor.centeredText(font(), getMessage(), getX() + getWidth() / 2, textY(), textColor);
    }
}
