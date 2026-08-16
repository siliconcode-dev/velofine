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

package dev.velofine.shaders.pipeline;

import com.mojang.blaze3d.shaders.ShaderType;
import dev.velofine.core.shader.ShaderSourceInterceptor;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * Registered alongside {@link GbuffersShaderInterceptor} (same {@link
 * dev.velofine.core.shader.ShaderSourceInterceptors#PRIORITY_SHADER_PACK} priority - the two never
 * claim the same identifier, since gbuffers substitution only matches real vanilla {@code
 * RenderPipelines} identifiers and this only matches synthetic {@code velofine:composite/...} ones
 * minted by {@link CompositePipelineBuilder}). Satisfies {@code ShaderSource.get(...)} entirely
 * from {@link CompositeProgramRegistry} - these identifiers have no real resource-pack asset behind
 * them at all.
 */
public final class CompositeShaderInterceptor implements ShaderSourceInterceptor {

    @Override
    public Optional<String> intercept(Identifier id, ShaderType type, String vanillaSource) {
        return Optional.ofNullable(CompositeProgramRegistry.get(id));
    }
}
