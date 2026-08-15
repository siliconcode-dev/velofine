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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A row that cycles through a fixed list of values - booleans, {@code Tri} overrides, governor
 * modes. One generic row rather than one class per value type, since they differ only in how the
 * value is spelled.
 */
public final class CycleRow<T> extends OptionRow {

    private final List<T> values;
    private final Function<T, String> formatter;
    private final Supplier<T> getter;
    private final Consumer<T> setter;
    private final T defaultValue;

    public CycleRow(int x, int y, int width, String label, String description, Applies applies,
            List<T> values, Function<T, String> formatter, Supplier<T> getter, Consumer<T> setter, T defaultValue) {
        super(x, y, width, label, description, applies);
        this.values = List.copyOf(values);
        this.formatter = formatter;
        this.getter = getter;
        this.setter = setter;
        this.defaultValue = defaultValue;
    }

    /** Convenience for the common on/off row. */
    public static CycleRow<Boolean> ofBoolean(int x, int y, int width, String label, String description,
            Applies applies, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultValue) {
        return new CycleRow<>(x, y, width, label, description, applies,
                List.of(Boolean.TRUE, Boolean.FALSE),
                value -> value ? "ON" : "OFF",
                getter, setter, defaultValue);
    }

    @Override
    protected Component valueText() {
        return Component.literal(formatter.apply(getter.get()));
    }

    @Override
    protected void activate(int direction) {
        int index = values.indexOf(getter.get());
        if (index < 0) {
            // Value came from a config file written by a different Velofine version, or from a
            // key we no longer offer - start from the beginning rather than throwing.
            setter.accept(values.get(0));
            return;
        }
        setter.accept(values.get(Math.floorMod(index + direction, values.size())));
    }

    @Override
    protected boolean valueIsOverride() {
        T current = getter.get();
        return current != null && !current.equals(defaultValue);
    }
}
