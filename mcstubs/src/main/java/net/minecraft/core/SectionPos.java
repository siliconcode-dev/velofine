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

package net.minecraft.core;

/**
 * Confirmed real static packed-long accessors via javap against the real 26.2 jar - used by
 * {@code RenderDistanceMixin} to read the Y component {@code SectionOcclusionGraph.isInViewDistance}
 * itself never reads (that method is confirmed X/Z-only via {@code -c} disassembly).
 */
public class SectionPos {

    public static int x(long packed) {
        throw new UnsupportedOperationException("stub");
    }

    public static int y(long packed) {
        throw new UnsupportedOperationException("stub");
    }

    public static int z(long packed) {
        throw new UnsupportedOperationException("stub");
    }
}
