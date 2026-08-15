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

package net.minecraft.client;

public class Options {

    public OptionInstance<Integer> renderDistance() {
        throw new UnsupportedOperationException("stub");
    }

    public OptionInstance<Integer> simulationDistance() {
        throw new UnsupportedOperationException("stub");
    }

    public int getEffectiveRenderDistance() {
        throw new UnsupportedOperationException("stub");
    }

    /** Confirmed real via javap: {@code IntRange(30, 110)}, default 70 - see {@code CameraMixin}. */
    public OptionInstance<Integer> fov() {
        throw new UnsupportedOperationException("stub");
    }

    /** Confirmed real via javap: {@code IntRange(0, 4)}, default 4. */
    public OptionInstance<Integer> mipmapLevels() {
        throw new UnsupportedOperationException("stub");
    }

    /** Confirmed real via javap: {@code IntRange(1, 3)}, default 2 (anisotropy bit-shift level). */
    public OptionInstance<Integer> maxAnisotropyBit() {
        throw new UnsupportedOperationException("stub");
    }

    /** Confirmed real via javap: boolean, default true. */
    public OptionInstance<Boolean> enableVsync() {
        throw new UnsupportedOperationException("stub");
    }

    public void save() {
        throw new UnsupportedOperationException("stub");
    }
}
