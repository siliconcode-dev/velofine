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

package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;

/**
 * Confirmed real via javap against the real 26.2 jar - the real class has many more methods
 * (render passes, buffer/texture copies, fences, ...); only the 6-arg
 * {@code writeToTexture(GpuTexture, NativeImage, int, int, int, int)} overload is stubbed. This is
 * the real, still-present <em>old</em> synchronous upload path MC-308593's broken staging-buffer
 * path (via {@code transientMemory().multiUploadStaging(...)} + {@code copyBufferToTexture(...)})
 * bypassed in 26.2 - LegacySupport's fix routes back through this method instead.
 */
public class CommandEncoder {

    public void writeToTexture(GpuTexture texture, NativeImage image, int mipLevel, int x, int y, int z) {
        throw new UnsupportedOperationException("stub");
    }
}
