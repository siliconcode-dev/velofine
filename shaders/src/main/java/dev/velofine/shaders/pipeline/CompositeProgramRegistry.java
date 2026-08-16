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

import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unlike gbuffers programs (which reuse <em>real</em> vanilla {@link RenderPipelines} shader
 * identifiers, mapped via {@link GbuffersProgramMapping}), a composite/deferred/final pass has no
 * real vanilla identifier of its own - {@code PostChainConfig.Pass} needs <em>some</em> {@code
 * Identifier} to hand to {@code vertexShaderId}/{@code fragmentShaderId}, so {@link
 * CompositePipelineBuilder} mints a synthetic one (namespace {@code velofine}) per pass and
 * registers its resolved GLSL text here. These identifiers never correspond to any real resource-
 * pack asset - {@code ShaderSource.get(...)} for them is entirely satisfied by {@link
 * CompositeShaderInterceptor} short-circuiting the lookup, so no real asset ever needs to exist at
 * that path.
 */
final class CompositeProgramRegistry {

    private static final Map<Identifier, String> sources = new ConcurrentHashMap<>();

    private CompositeProgramRegistry() {
    }

    static Identifier register(String passName, String suffix, String glslSource) {
        Identifier id = Identifier.fromNamespaceAndPath("velofine", "composite/" + passName + suffix);
        sources.put(id, glslSource);
        return id;
    }

    static String get(Identifier id) {
        return sources.get(id);
    }

    static void clear() {
        sources.clear();
    }
}
