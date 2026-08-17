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

/**
 * The black/white/red brutalist palette and the handful of primitives every Velofine screen draws
 * with. Deliberately nothing but flat {@code fill} rectangles - no vanilla sprites, no gradients,
 * no rounded corners - so the in-game UI reads as the same object as the installer.
 *
 * <p>Colours are ARGB ints, which is what {@code GuiGraphicsExtractor.fill} expects.
 */
public final class VelofineTheme {

    /** Full-screen backdrop. Slightly translucent so the world/menu behind stays faintly visible. */
    public static final int BACKDROP = 0xF00A0A0A;
    public static final int PANEL = 0xFF121212;
    public static final int PANEL_RAISED = 0xFF1C1C1C;
    public static final int BORDER = 0xFF2E2E2E;
    public static final int BORDER_BRIGHT = 0xFFF2F2F2;

    public static final int ACCENT = 0xFFD01B1B;
    public static final int ACCENT_DIM = 0xFF5E0D0D;

    /**
     * Muted amber - "needs attention/restart," never the red brand accent. Red stays reserved for
     * selection/override/primary-action (see {@code Applies.RESTART}'s badge in
     * {@code VelofineConfigScreen.drawInfoBox}, the first call site to actually use this).
     */
    public static final int WARNING = 0xFFB8860B;
    /** Muted green - "recommended for your hardware" / healthy. Not yet consumed (Phase E/F). */
    public static final int SUCCESS = 0xFF4C8F5B;

    public static final int TEXT = 0xFFF2F2F2;
    public static final int TEXT_DIM = 0xFF8A8A8A;
    public static final int TEXT_DISABLED = 0xFF5A5A5A;

    /** Height of a tab-rail entry, chosen to match vanilla's 20px button rhythm. */
    public static final int ROW_HEIGHT = 20;
    /**
     * Height of a settings row. Deliberately decoupled from {@link #ROW_HEIGHT} (v1.5 UI overhaul):
     * rows now render two lines (name+value, then an inline description/slider line), which needs
     * more room than a tab-rail entry - growing this no longer forces the tab rail to grow with it.
     */
    public static final int CONTENT_ROW_HEIGHT = 34;
    /** Visible gap between rows - deliberately more than a hairline so a dense page still breathes. */
    public static final int ROW_SPACING = 6;
    public static final int PADDING = 6;

    private VelofineTheme() {
    }

    /** One-pixel outline, drawn as four fills (the extractor has no stroke primitive). */
    public static void border(GuiGraphicsExtractor extractor, int x, int y, int width, int height, int color) {
        extractor.fill(x, y, x + width, y + 1, color);
        extractor.fill(x, y + height - 1, x + width, y + height, color);
        extractor.fill(x, y + 1, x + 1, y + height - 1, color);
        extractor.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public static void panel(GuiGraphicsExtractor extractor, int x, int y, int width, int height, int fill,
            int borderColor) {
        extractor.fill(x, y, x + width, y + height, fill);
        border(extractor, x, y, width, height, borderColor);
    }

    /**
     * The red marker block that flags the selected/active thing. Brutalist UI leans on solid blocks
     * of colour rather than highlight glows, and this is the one motif reused everywhere.
     */
    public static void marker(GuiGraphicsExtractor extractor, int x, int y, int height) {
        extractor.fill(x, y, x + 3, y + height, ACCENT);
    }

    public static void horizontalRule(GuiGraphicsExtractor extractor, int x, int y, int width, int color) {
        extractor.fill(x, y, x + width, y + 1, color);
    }
}
