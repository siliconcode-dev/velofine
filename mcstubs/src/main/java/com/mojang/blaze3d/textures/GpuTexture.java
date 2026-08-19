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

package com.mojang.blaze3d.textures;

/**
 * Confirmed real via javap against the real 26.2 jar - the real class has several more instance
 * methods (getWidth/getHeight/getFormat/...) Velofine's own code never calls; only the two real
 * {@code USAGE_*} bit-flag constants LegacySupport's animated-texture-upload fix passes to
 * {@code GpuDevice.createTexture} are stubbed alongside the bare type itself.
 */
public abstract class GpuTexture implements AutoCloseable {

    public static final int USAGE_COPY_DST = 0;
    public static final int USAGE_TEXTURE_BINDING = 0;
}
