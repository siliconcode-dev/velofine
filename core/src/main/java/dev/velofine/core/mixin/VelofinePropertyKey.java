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

package dev.velofine.core.mixin;

import org.spongepowered.asm.service.IPropertyKey;

import java.util.Objects;

/** Trivial name-wrapping key for {@link VelofinePropertyService}'s blackboard. */
public final class VelofinePropertyKey implements IPropertyKey {

    private final String name;

    public VelofinePropertyKey(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VelofinePropertyKey other && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
