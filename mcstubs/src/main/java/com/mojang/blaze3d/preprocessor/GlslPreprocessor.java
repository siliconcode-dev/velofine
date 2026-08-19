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

package com.mojang.blaze3d.preprocessor;

import net.minecraft.client.renderer.ShaderDefines;

/**
 * Confirmed real via javap against the real 26.2 jar - the real class also has instance
 * {@code process}/{@code applyImport} members Velofine never touches; only the static
 * {@code injectDefines} is stubbed.
 *
 * <p>This is the seam that fixes v1.7-Beta's end-portal bug. {@code GlDevice.compileShader} calls
 * {@code ShaderSource.get(...)} (offset 9) and only then
 * {@code GlslPreprocessor.injectDefines(source, defines)} (offset 47), so a shader's
 * {@code #define}s - e.g. {@code PORTAL_LAYERS 16}, which lives on
 * {@code RenderPipelines.END_PORTAL}'s ShaderDefines and <em>not</em> in the raw {@code .fsh} - do
 * not exist in the source text until after this call. {@code core.mixin.GlDeviceMixin} redirects it
 * so the post-define stage sees exactly what the GL compiler will see.
 */
public abstract class GlslPreprocessor {

    public static String injectDefines(String source, ShaderDefines defines) {
        throw new UnsupportedOperationException("stub");
    }
}
