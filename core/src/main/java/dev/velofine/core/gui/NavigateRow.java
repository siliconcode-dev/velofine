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

import net.minecraft.network.chat.Component;

/**
 * A row that opens a sub-screen instead of cycling a value - {@code OptionRow}'s click-to-step
 * model (left half = back, right half = forward) doesn't fit "drill into a sub-screen", so both
 * halves trigger the same action here. First use: the Utility page's "Shader options &gt;" row
 * (Phase 7.7), which opens {@code ShaderOptionsScreen}.
 */
public final class NavigateRow extends OptionRow {

    private final String valueLabel;
    private final Runnable onActivate;

    public NavigateRow(int x, int y, int width, String label, String description, String valueLabel, Runnable onActivate) {
        super(x, y, width, label, description, Applies.LIVE);
        this.valueLabel = valueLabel;
        this.onActivate = onActivate;
    }

    @Override
    protected Component valueText() {
        return Component.literal(valueLabel);
    }

    @Override
    protected void activate(int direction) {
        onActivate.run();
    }
}
