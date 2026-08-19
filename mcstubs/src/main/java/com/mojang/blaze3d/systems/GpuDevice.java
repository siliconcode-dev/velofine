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

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

import java.util.function.Supplier;

/**
 * Confirmed real via javap against the real 26.2 jar - the real class has many more methods
 * (samplers, buffers, pipeline precompilation, ...); only the three LegacySupport's
 * animated-texture-upload fix calls (via {@code RenderSystem.getDevice()}) are stubbed. The
 * {@code Supplier<String>}-overload of {@code createTexture} (not the plain-{@code String} one) is
 * the one confirmed used by the fix's per-frame texture labels.
 */
public class GpuDevice {

    public CommandEncoder createCommandEncoder() {
        throw new UnsupportedOperationException("stub");
    }

    public GpuTexture createTexture(Supplier<String> label, int usage, GpuFormat format, int width, int height,
            int depthOrLayers, int mipLevels) {
        throw new UnsupportedOperationException("stub");
    }

    public GpuTextureView createTextureView(GpuTexture texture) {
        throw new UnsupportedOperationException("stub");
    }
}
