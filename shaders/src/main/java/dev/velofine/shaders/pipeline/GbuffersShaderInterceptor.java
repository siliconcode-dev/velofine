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
import dev.velofine.shaders.format.ShaderPack;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * Registered into {@code core}'s {@code ShaderSourceInterceptors} (at {@link
 * ShaderSourceInterceptors#PRIORITY_SHADER_PACK}, ahead of LegacySupport's {@code mix()} patch -
 * see that registry's class javadoc for why) whenever Utility's shader feature is enabled. Not
 * interested (returns empty) unless {@link ShaderPipelineState} has an active pack <em>and</em>
 * that pack's shader identifier maps to a {@code gbuffers_*} program the pack actually provides.
 */
public final class GbuffersShaderInterceptor implements ShaderSourceInterceptor {

    @Override
    public Optional<String> intercept(Identifier id, ShaderType type, String vanillaSource) {
        ShaderPack pack = ShaderPipelineState.getActivePack();
        if (pack == null) {
            return Optional.empty();
        }

        GbuffersProgramMapping mapping = ShaderPipelineState.getMapping();
        String program = type == ShaderType.VERTEX
                ? mapping.programForVertexShader(id)
                : mapping.programForFragmentShader(id);
        if (program == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(ShaderProgramLoader.load(pack, program, type));
    }
}
