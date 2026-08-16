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

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps real vanilla {@link RenderPipeline} shader {@link Identifier}s to the OptiFine {@code
 * gbuffers_*} program name a shader pack should render that draw category with.
 *
 * <p>Built by reading each mapped {@code RenderPipelines} constant's own {@link
 * RenderPipeline#getVertexShader()}/{@link RenderPipeline#getFragmentShader()} at runtime, rather
 * than hardcoding the underlying {@code Identifier} string values - those are real vanilla
 * implementation details this class has no need to guess at or keep in sync with by hand. This is
 * what makes the whole gbuffers-interception approach tractable: CLAUDE.md's Phase 7 research notes
 * confirm every terrain/entity/particle/etc. draw call already references one of a fixed, cataloged
 * set of 87 named {@code RenderPipelines} constants, so redirecting shader source for the ones that
 * matter is a static table, not a patch on every draw-call site.
 *
 * <p><b>Deliberately left unmapped, not guessed at</b>: {@code END_PORTAL}/{@code END_GATEWAY}/
 * {@code BEACON_BEAM_*} have no established OptiFine {@code gbuffers_*} equivalent for these
 * niche, highly custom-rendered effects - they render with vanilla's own shaders unconditionally,
 * the same as any other unmapped pipeline, rather than a wrong guess.
 *
 * <p><b>Open question, flagged rather than assumed</b>: {@code ITEM_CUTOUT}/{@code ITEM_TRANSLUCENT}
 * are mapped to {@code gbuffers_hand} here, but the real pipeline is used for held items, dropped
 * item entities, and item-frame contents alike - vanilla has no dedicated "this is specifically the
 * first-person hand" pipeline the way the mapping below implies. Needs real visual verification once
 * 7.4 actually runs against a shader pack; may need a runtime check (e.g. camera perspective) rather
 * than a static pipeline-identity mapping if dropped items render incorrectly through
 * {@code gbuffers_hand}.
 */
public final class GbuffersProgramMapping {

    public static final String GBUFFERS_TERRAIN = "gbuffers_terrain";
    public static final String GBUFFERS_WATER = "gbuffers_water";
    public static final String GBUFFERS_BLOCK = "gbuffers_block";
    public static final String GBUFFERS_ENTITIES = "gbuffers_entities";
    public static final String GBUFFERS_HAND = "gbuffers_hand";
    public static final String GBUFFERS_WEATHER = "gbuffers_weather";
    public static final String GBUFFERS_SKYBASIC = "gbuffers_skybasic";
    public static final String GBUFFERS_CLOUDS = "gbuffers_clouds";
    public static final String GBUFFERS_TEXTURED = "gbuffers_textured";

    private final Map<Identifier, String> vertexShaderToProgram = new HashMap<>();
    private final Map<Identifier, String> fragmentShaderToProgram = new HashMap<>();

    public GbuffersProgramMapping() {
        map(GBUFFERS_TERRAIN, RenderPipelines.SOLID_TERRAIN, RenderPipelines.CUTOUT_TERRAIN);
        map(GBUFFERS_WATER, RenderPipelines.TRANSLUCENT_TERRAIN);
        map(GBUFFERS_BLOCK, RenderPipelines.SOLID_BLOCK, RenderPipelines.CUTOUT_BLOCK, RenderPipelines.TRANSLUCENT_BLOCK);
        map(GBUFFERS_ENTITIES,
                RenderPipelines.ENTITY_SOLID, RenderPipelines.ENTITY_SOLID_Z_OFFSET_FORWARD,
                RenderPipelines.ENTITY_CUTOUT_CULL, RenderPipelines.ENTITY_CUTOUT,
                RenderPipelines.ENTITY_CUTOUT_Z_OFFSET, RenderPipelines.ENTITY_CUTOUT_DISSOLVE,
                RenderPipelines.ENTITY_TRANSLUCENT, RenderPipelines.ENTITY_TRANSLUCENT_EMISSIVE,
                RenderPipelines.ENTITY_TRANSLUCENT_CULL,
                RenderPipelines.ARMOR_CUTOUT_NO_CULL, RenderPipelines.ARMOR_DECAL_CUTOUT_NO_CULL,
                RenderPipelines.ARMOR_TRANSLUCENT);
        map(GBUFFERS_HAND, RenderPipelines.ITEM_CUTOUT, RenderPipelines.ITEM_TRANSLUCENT);
        map(GBUFFERS_WEATHER, RenderPipelines.WEATHER_DEPTH_WRITE, RenderPipelines.WEATHER_NO_DEPTH_WRITE);
        map(GBUFFERS_SKYBASIC, RenderPipelines.SKY, RenderPipelines.END_SKY,
                RenderPipelines.SUNRISE_SUNSET, RenderPipelines.STARS, RenderPipelines.CELESTIAL);
        map(GBUFFERS_CLOUDS, RenderPipelines.FLAT_CLOUDS, RenderPipelines.CLOUDS);
        map(GBUFFERS_TEXTURED, RenderPipelines.OPAQUE_PARTICLE, RenderPipelines.TRANSLUCENT_PARTICLE);
    }

    private void map(String gbuffersProgram, RenderPipeline... pipelines) {
        for (RenderPipeline pipeline : pipelines) {
            vertexShaderToProgram.put(pipeline.getVertexShader(), gbuffersProgram);
            fragmentShaderToProgram.put(pipeline.getFragmentShader(), gbuffersProgram);
        }
    }

    /** @return the gbuffers program name a shader pack should provide for this shader source identifier, or {@code null} if unmapped. */
    public String programForVertexShader(Identifier id) {
        return vertexShaderToProgram.get(id);
    }

    public String programForFragmentShader(Identifier id) {
        return fragmentShaderToProgram.get(id);
    }
}
