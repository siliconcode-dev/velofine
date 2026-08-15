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

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** A clamped integer row: click/scroll steps the value, never wrapping past its bounds. */
public final class IntRow extends OptionRow {

    private final int min;
    private final int max;
    private final int step;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final String suffix;
    private final int defaultValue;

    public IntRow(int x, int y, int width, String label, String description, Applies applies,
            int min, int max, int step, String suffix, IntSupplier getter, IntConsumer setter, int defaultValue) {
        super(x, y, width, label, description, applies);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = defaultValue;
    }

    @Override
    protected Component valueText() {
        return Component.literal(getter.getAsInt() + suffix);
    }

    @Override
    protected void activate(int direction) {
        int next = getter.getAsInt() + direction * step;
        setter.accept(Math.max(min, Math.min(max, next)));
    }

    @Override
    protected boolean valueIsOverride() {
        return getter.getAsInt() != defaultValue;
    }
}
