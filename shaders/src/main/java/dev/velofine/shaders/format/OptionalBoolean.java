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

package dev.velofine.shaders.format;

import java.util.function.BooleanSupplier;

/**
 * A tri-state boolean distinguishing "the pack didn't say" from an explicit true/false - a
 * shaders.properties directive left unset must fall back to Velofine's own default, not silently
 * become {@code false}. Adapted from IrisShaders/Iris ({@code common/.../helpers/OptionalBoolean.java}),
 * LGPL-3.0.
 */
public enum OptionalBoolean {
    UNSET,
    FALSE,
    TRUE;

    public boolean orElse(boolean defaultValue) {
        return this == UNSET ? defaultValue : this == TRUE;
    }

    public boolean orElseGet(BooleanSupplier defaultValue) {
        return this == UNSET ? defaultValue.getAsBoolean() : this == TRUE;
    }

    public static OptionalBoolean of(boolean value) {
        return value ? TRUE : FALSE;
    }
}
