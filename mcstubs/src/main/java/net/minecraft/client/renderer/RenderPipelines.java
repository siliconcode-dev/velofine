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

import com.mojang.blaze3d.pipeline.RenderPipeline;

/**
 * Confirmed real via javap against the real 26.2 jar - a real catalog class of 87 named {@code
 * public static final RenderPipeline} constants, one per vanilla draw category (terrain, entities,
 * particles, sky, clouds, weather, GUI, text, ...). Only the subset {@code
 * dev.velofine.shaders.pipeline.GbuffersProgramMapping} actually maps to an OptiFine {@code
 * gbuffers_*} program is stubbed here - see that class for the real→gbuffers mapping table and
 * javadoc on the fields intentionally left unmapped (GUI/text/debug pipelines have no gbuffers
 * equivalent; hand rendering has no dedicated pipeline of its own, flagged there as an open
 * question rather than guessed at).
 */
public final class RenderPipelines {

    private RenderPipelines() {
    }

    public static final RenderPipeline SOLID_BLOCK = new RenderPipeline();
    public static final RenderPipeline SOLID_TERRAIN = new RenderPipeline();
    public static final RenderPipeline CUTOUT_BLOCK = new RenderPipeline();
    public static final RenderPipeline CUTOUT_TERRAIN = new RenderPipeline();
    public static final RenderPipeline TRANSLUCENT_TERRAIN = new RenderPipeline();
    public static final RenderPipeline TRANSLUCENT_BLOCK = new RenderPipeline();

    public static final RenderPipeline ARMOR_CUTOUT_NO_CULL = new RenderPipeline();
    public static final RenderPipeline ARMOR_DECAL_CUTOUT_NO_CULL = new RenderPipeline();
    public static final RenderPipeline ARMOR_TRANSLUCENT = new RenderPipeline();

    public static final RenderPipeline ENTITY_SOLID = new RenderPipeline();
    public static final RenderPipeline ENTITY_SOLID_Z_OFFSET_FORWARD = new RenderPipeline();
    public static final RenderPipeline ENTITY_CUTOUT_CULL = new RenderPipeline();
    public static final RenderPipeline ENTITY_CUTOUT = new RenderPipeline();
    public static final RenderPipeline ENTITY_CUTOUT_Z_OFFSET = new RenderPipeline();
    public static final RenderPipeline ENTITY_CUTOUT_DISSOLVE = new RenderPipeline();
    public static final RenderPipeline ENTITY_TRANSLUCENT = new RenderPipeline();
    public static final RenderPipeline ENTITY_TRANSLUCENT_EMISSIVE = new RenderPipeline();
    public static final RenderPipeline ENTITY_TRANSLUCENT_CULL = new RenderPipeline();

    public static final RenderPipeline ITEM_CUTOUT = new RenderPipeline();
    public static final RenderPipeline ITEM_TRANSLUCENT = new RenderPipeline();

    public static final RenderPipeline WEATHER_DEPTH_WRITE = new RenderPipeline();
    public static final RenderPipeline WEATHER_NO_DEPTH_WRITE = new RenderPipeline();

    public static final RenderPipeline SKY = new RenderPipeline();
    public static final RenderPipeline END_SKY = new RenderPipeline();
    public static final RenderPipeline SUNRISE_SUNSET = new RenderPipeline();
    public static final RenderPipeline STARS = new RenderPipeline();
    public static final RenderPipeline CELESTIAL = new RenderPipeline();

    public static final RenderPipeline FLAT_CLOUDS = new RenderPipeline();
    public static final RenderPipeline CLOUDS = new RenderPipeline();

    public static final RenderPipeline OPAQUE_PARTICLE = new RenderPipeline();
    public static final RenderPipeline TRANSLUCENT_PARTICLE = new RenderPipeline();

    public static final RenderPipeline END_PORTAL = new RenderPipeline();
    public static final RenderPipeline END_GATEWAY = new RenderPipeline();

    public static final RenderPipeline BEACON_BEAM_OPAQUE = new RenderPipeline();
    public static final RenderPipeline BEACON_BEAM_TRANSLUCENT = new RenderPipeline();
}
