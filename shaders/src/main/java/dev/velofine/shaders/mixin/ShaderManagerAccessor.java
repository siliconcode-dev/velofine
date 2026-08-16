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

package dev.velofine.shaders.mixin;

import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code ShaderManager}'s two private fields dedicated to post-chain rendering (confirmed
 * real via javap - named exactly {@code postChainProjection}/{@code postChainProjectionMatrixBuffer},
 * distinct from the level's own 3D projection). {@code @Accessor} mixins only generate a getter
 * bytecode-equivalent to a direct field read - no behavior change to the target class, the
 * lowest-risk Mixin pattern available (same category CLAUDE.md's Mixin section already establishes
 * for pure side-effect injections).
 */
@Mixin(targets = "net.minecraft.client.renderer.ShaderManager")
public interface ShaderManagerAccessor {

    @Accessor("postChainProjection")
    Projection velofine$getPostChainProjection();

    @Accessor("postChainProjectionMatrixBuffer")
    ProjectionMatrixBuffer velofine$getPostChainProjectionMatrixBuffer();
}
