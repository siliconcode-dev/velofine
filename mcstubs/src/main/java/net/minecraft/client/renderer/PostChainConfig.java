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

import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Confirmed real via javap against the real 26.2 jar - a real Java record (with a Mojang {@code
 * Codec}, so it is JSON-loadable the same way {@code assets/velofine/post_effect/fxaa.json}
 * already is), a substantially more capable multi-pass post-processing config than Phase 6's
 * single-slot {@code GameRenderer.setPostEffect} mechanism. Named internal targets plus an ordered
 * pass list is a strong real foundation for Phase 7's composite/deferred/final chaining.
 *
 * <p><b>Real constraint worth flagging, not silently worked around</b>: {@link Pass#outputTarget()}
 * is a single {@link Identifier}, not a list - OptiFine's {@code DRAWBUFFERS} directive lets one
 * composite/deferred pass write to <em>multiple</em> colortex buffers at once, which this real
 * Mojang model has no direct equivalent for. See {@code
 * dev.velofine.shaders.pipeline.CompositePipelineBuilder}'s class javadoc for how v1 handles this.
 */
public final class PostChainConfig {

    public PostChainConfig(Map<Identifier, InternalTarget> internalTargets, List<Pass> passes) {
    }

    public Map<Identifier, InternalTarget> internalTargets() {
        throw new UnsupportedOperationException("stub");
    }

    public List<Pass> passes() {
        throw new UnsupportedOperationException("stub");
    }

    public static final class InternalTarget {
        public InternalTarget(Optional<Integer> width, Optional<Integer> height, boolean persistent, int clearColor) {
        }
    }

    public static final class Pass {
        public Pass(Identifier vertexShaderId, Identifier fragmentShaderId, List<Input> inputs,
                Identifier outputTarget, Map<String, List<UniformValue>> uniforms) {
        }

        public Identifier vertexShaderId() {
            throw new UnsupportedOperationException("stub");
        }

        public Identifier fragmentShaderId() {
            throw new UnsupportedOperationException("stub");
        }

        public List<Input> inputs() {
            throw new UnsupportedOperationException("stub");
        }

        public Identifier outputTarget() {
            throw new UnsupportedOperationException("stub");
        }

        public Map<String, List<UniformValue>> uniforms() {
            throw new UnsupportedOperationException("stub");
        }
    }

    public interface Input {

        String samplerName();

        Set<Identifier> referencedTargets();
    }

    public static final class TargetInput implements Input {
        public TargetInput(String samplerName, Identifier targetId, boolean useDepthBuffer, boolean bilinear) {
        }

        @Override
        public String samplerName() {
            throw new UnsupportedOperationException("stub");
        }

        @Override
        public Set<Identifier> referencedTargets() {
            throw new UnsupportedOperationException("stub");
        }
    }

    public static final class TextureInput implements Input {
        public TextureInput(String samplerName, Identifier location, int width, int height, boolean bilinear) {
        }

        @Override
        public String samplerName() {
            throw new UnsupportedOperationException("stub");
        }

        @Override
        public Set<Identifier> referencedTargets() {
            throw new UnsupportedOperationException("stub");
        }
    }
}
