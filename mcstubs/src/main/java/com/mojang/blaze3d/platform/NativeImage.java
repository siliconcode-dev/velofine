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

package com.mojang.blaze3d.platform;

/**
 * Confirmed real via javap against the real 26.2 jar - only the {@code (int, int, boolean)}
 * constructor and the 9-arg {@code copyRect} overload LegacySupport's animated-texture-upload fix
 * actually uses are stubbed; the real class has several other constructors/read/write methods.
 */
public final class NativeImage implements AutoCloseable {

    public NativeImage(int width, int height, boolean useStbFree) {
    }

    public void copyRect(NativeImage dest, int srcX, int srcY, int destX, int destY, int width, int height,
            boolean mirrorX, boolean mirrorY) {
        throw new UnsupportedOperationException("stub");
    }

    @Override
    public void close() {
    }
}
