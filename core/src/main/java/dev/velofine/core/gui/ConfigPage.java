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

import dev.velofine.core.config.VelofineConfig;

import java.util.List;

/**
 * One engine's config panel.
 *
 * <p>Masterdoc 3 asks for each engine to be independently toggleable "via its own GUI config
 * panel (not one combined settings screen)". Each page here is exactly that - its own panel, with
 * its own master toggle - reached through a tab rail rather than by walking back out to a menu.
 *
 * <p>Pages always build against a <em>working copy</em> of the config, never the live one, so
 * nothing takes effect until the user presses APPLY.
 */
public abstract class ConfigPage {

    /** Shown in the tab rail. */
    public abstract String title();

    public abstract List<OptionRow> buildRows(VelofineConfig working, RowCursor cursor);

    /** Whether this page's engine is currently switched on, for the rail's dimmed state. */
    public abstract boolean engineEnabled(VelofineConfig working);

    /** Optional line under the option list - live read-outs, or a note about what is not built yet. */
    public String footnote() {
        return null;
    }

    /** Hands out sequential row rectangles so pages don't repeat layout arithmetic. */
    public static final class RowCursor {

        private final int x;
        private final int width;
        private int y;

        public RowCursor(int x, int y, int width) {
            this.x = x;
            this.y = y;
            this.width = width;
        }

        public int x() {
            return x;
        }

        public int width() {
            return width;
        }

        /** Y for the next row, advancing the cursor past it. */
        public int nextY() {
            int current = y;
            y += VelofineTheme.ROW_HEIGHT + VelofineTheme.ROW_SPACING;
            return current;
        }
    }
}
