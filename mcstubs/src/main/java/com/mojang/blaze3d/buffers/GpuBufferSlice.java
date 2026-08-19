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

package com.mojang.blaze3d.buffers;

/**
 * Confirmed real via javap against the real 26.2 jar - the real type is a record with
 * {@code buffer()}/{@code offset()}/{@code length()} accessors and a {@code map(...)} method too;
 * only {@code slice(long, long)} (the one LegacySupport's animated-texture-upload fix calls, to
 * carve one mip level's UBO region out of the shared slice vanilla passes into
 * {@code createAnimationState}) is stubbed.
 */
public final class GpuBufferSlice {

    public GpuBufferSlice slice(long offset, long length) {
        throw new UnsupportedOperationException("stub");
    }
}
