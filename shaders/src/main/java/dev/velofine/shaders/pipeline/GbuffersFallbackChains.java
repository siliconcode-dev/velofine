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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * If a shader pack doesn't define a given {@code gbuffers_*} program, OptiFine falls back to a more
 * generic one rather than leaving the draw category unshaded - this is why most real packs only
 * ship a handful of {@code gbuffers_*} files and still cover every draw category. Chains below are
 * OptiFine's publicly documented fallback order (shaderLABS' "Rendering Pipeline" reference and
 * OptiFine's own {@code shaders.txt}), reproduced here as plain data (not copied source) - **best
 * effort, not exhaustively verified against every real shader pack's actual fallback behavior**;
 * revisit if BSL/Complementary (Build_plan's validation targets) render incorrectly for a program
 * this table resolves differently than OptiFine/Iris would.
 */
final class GbuffersFallbackChains {

    private static final Map<String, List<String>> CHAINS = Map.of(
            GbuffersProgramMapping.GBUFFERS_WATER, List.of(GbuffersProgramMapping.GBUFFERS_TERRAIN),
            GbuffersProgramMapping.GBUFFERS_BLOCK, List.of(GbuffersProgramMapping.GBUFFERS_TERRAIN),
            GbuffersProgramMapping.GBUFFERS_TERRAIN, List.of("gbuffers_textured_lit", GbuffersProgramMapping.GBUFFERS_TEXTURED, "gbuffers_basic"),
            GbuffersProgramMapping.GBUFFERS_ENTITIES, List.of("gbuffers_textured_lit", GbuffersProgramMapping.GBUFFERS_TEXTURED, "gbuffers_basic"),
            GbuffersProgramMapping.GBUFFERS_HAND, List.of("gbuffers_textured_lit", GbuffersProgramMapping.GBUFFERS_TEXTURED, "gbuffers_basic"),
            GbuffersProgramMapping.GBUFFERS_WEATHER, List.of(GbuffersProgramMapping.GBUFFERS_TEXTURED, "gbuffers_basic"),
            GbuffersProgramMapping.GBUFFERS_CLOUDS, List.of("gbuffers_basic"),
            GbuffersProgramMapping.GBUFFERS_SKYBASIC, List.of("gbuffers_basic"),
            GbuffersProgramMapping.GBUFFERS_TEXTURED, List.of("gbuffers_basic"),
            "gbuffers_textured_lit", List.of(GbuffersProgramMapping.GBUFFERS_TEXTURED, "gbuffers_basic"));

    private GbuffersFallbackChains() {
    }

    /** The program itself, then each fallback in order, ending at {@code gbuffers_basic}. */
    static List<String> resolutionOrder(String program) {
        List<String> order = new ArrayList<>();
        order.add(program);
        order.addAll(CHAINS.getOrDefault(program, List.of()));
        if (!order.contains("gbuffers_basic")) {
            order.add("gbuffers_basic");
        }
        return order;
    }
}
