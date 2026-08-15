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

/**
 * An integer row: click/scroll steps the value. Clamped at the bounds by default; a row can opt
 * into wrapping instead (past {@code max} rolls to {@code min} and vice versa) via the extra-arg
 * constructor - useful for a value like zoom level where "one more click past the top" reading as
 * dead/stuck is worse than looping back around.
 */
public final class IntRow extends OptionRow {

    private final int min;
    private final int max;
    private final int step;
    private final IntSupplier getter;
    private final IntConsumer setter;
    private final String suffix;
    private final int defaultValue;
    private final boolean wrap;

    public IntRow(int x, int y, int width, String label, String description, Applies applies,
            int min, int max, int step, String suffix, IntSupplier getter, IntConsumer setter, int defaultValue) {
        this(x, y, width, label, description, applies, min, max, step, suffix, getter, setter, defaultValue, false);
    }

    public IntRow(int x, int y, int width, String label, String description, Applies applies,
            int min, int max, int step, String suffix, IntSupplier getter, IntConsumer setter, int defaultValue,
            boolean wrap) {
        super(x, y, width, label, description, applies);
        this.min = min;
        this.max = max;
        this.step = step;
        this.suffix = suffix;
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = defaultValue;
        this.wrap = wrap;
    }

    @Override
    protected Component valueText() {
        return Component.literal(getter.getAsInt() + suffix);
    }

    @Override
    protected void activate(int direction) {
        int next = getter.getAsInt() + direction * step;
        if (wrap) {
            if (next > max) {
                next = min;
            } else if (next < min) {
                next = max;
            }
            setter.accept(next);
        } else {
            setter.accept(Math.max(min, Math.min(max, next)));
        }
    }

    @Override
    protected boolean valueIsOverride() {
        return getter.getAsInt() != defaultValue;
    }
}
