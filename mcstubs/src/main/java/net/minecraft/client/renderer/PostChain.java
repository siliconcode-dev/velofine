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

package net.minecraft.client.renderer;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.resources.Identifier;

import java.util.Set;

/**
 * Confirmed real via javap and, for the real integration path, via {@code -c} disassembly of
 * {@code GameRenderer.render(...)} - vanilla's own built-in post effects (blur, the configurable
 * {@code postEffectId}) go through exactly {@code ShaderManager.getPostChain(id, targets)} then
 * {@code postChain.process(mainRenderTarget, resourcePool)}. Velofine's dynamically-built shader-
 * pack composite chain bypasses {@code getPostChain} (asset-cache-only) and calls {@link #load}
 * directly instead - see {@link ShaderManager}'s own javadoc for why.
 */
public class PostChain implements AutoCloseable {

    /** Confirmed real public static field via javap. */
    public static Identifier MAIN_TARGET_ID;

    public static PostChain load(PostChainConfig config, net.minecraft.client.renderer.texture.TextureManager textureManager,
            Set<Identifier> externalTargets, Identifier mainTargetId, Projection projection, ProjectionMatrixBuffer projectionMatrixBuffer)
            throws ShaderManager.CompilationException {
        throw new UnsupportedOperationException("stub");
    }

    public void process(RenderTarget mainTarget, GraphicsResourceAllocator allocator) {
        throw new UnsupportedOperationException("stub");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("stub");
    }
}
