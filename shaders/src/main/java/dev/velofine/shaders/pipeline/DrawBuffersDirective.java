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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses OptiFine's {@code /* DRAWBUFFERS:012 *}{@code /} and the newer {@code /* RENDERTARGETS:
 * 0,1,2 *}{@code /} GLSL comment directives, which declare which {@code colortexN} buffers a
 * composite/deferred/gbuffers fragment shader writes its outputs to, in output-slot order (the
 * first digit/index is {@code gl_FragData[0]}, the second is {@code gl_FragData[1]}, etc.).
 */
final class DrawBuffersDirective {

    private static final Pattern DRAWBUFFERS = Pattern.compile("/\\*\\s*DRAWBUFFERS:\\s*([0-9A-F]+)\\s*\\*/");
    private static final Pattern RENDERTARGETS = Pattern.compile("/\\*\\s*RENDERTARGETS:\\s*([0-9,\\s]+)\\s*\\*/");

    private DrawBuffersDirective() {
    }

    /** @return the declared output colortex indices in slot order, or a single-element {@code [0]} if no directive is present (OptiFine's own default). */
    static List<Integer> parse(String fragmentSource) {
        Matcher renderTargets = RENDERTARGETS.matcher(fragmentSource);
        if (renderTargets.find()) {
            List<Integer> indices = new ArrayList<>();
            for (String part : renderTargets.group(1).split(",")) {
                if (!part.isBlank()) {
                    indices.add(Integer.parseInt(part.trim()));
                }
            }
            return indices;
        }

        Matcher drawBuffers = DRAWBUFFERS.matcher(fragmentSource);
        if (drawBuffers.find()) {
            List<Integer> indices = new ArrayList<>();
            for (char c : drawBuffers.group(1).toCharArray()) {
                indices.add(Character.digit(c, 16));
            }
            return indices;
        }

        return List.of(0);
    }
}
