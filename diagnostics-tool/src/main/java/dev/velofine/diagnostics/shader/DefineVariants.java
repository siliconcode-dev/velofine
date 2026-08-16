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

package dev.velofine.diagnostics.shader;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks which real core shaders back the v1.5 bug report's symptoms, and which
 * {@code #ifdef}-gated preprocessor-define variants of them are worth compile-testing separately -
 * plus which shaders need a define <b>unconditionally</b> injected just to compile at all.
 *
 * <p>The v1.5 spec docs name {@code rendertype_solid}/{@code rendertype_translucent}/
 * {@code rendertype_cutout} as the prime suspects for invisible lava/water/portals and black
 * blocks - but those files do not exist in the real 26.2 client jar (confirmed by extracting and
 * listing {@code assets/minecraft/shaders/core/}). Solid/cutout/translucent terrain all share one
 * real file pair, {@code terrain.fsh}/{@code terrain.vsh}, differentiated at compile time by a real,
 * confirmed {@code #ifdef ALPHA_CUTOUT} branch in {@code terrain.fsh} (Mojang's own
 * {@code RenderPipeline.getShaderDefines()} injects this define for the cutout variant at runtime -
 * it is not present in the raw file). Portals are {@code rendertype_end_portal}, which does exist
 * under that exact name.
 *
 * <p>Only these two confirmed-by-reading-real-bytes cases are specially handled - a full catalog of
 * every {@code RenderPipeline}'s exact {@code ShaderDefines} across all ~30 core shader pairs is a
 * much larger undertaking (see the v1 CLAUDE.md's own Phase 7 notes on this system) and is left for
 * a future pass if real tester reports point at a different symptom this doesn't cover.
 */
public final class DefineVariants {

    public static final String DEFAULT_VARIANT = "default";
    private static final String ALPHA_CUTOUT_VARIANT = "ALPHA_CUTOUT";

    /** The real shaders confirmed to back the bug report's named symptoms. */
    private static final Set<String> FLAGGED_SHADERS = Set.of("terrain", "rendertype_end_portal");

    private static final Map<String, List<String>> VARIANTS_BY_SHADER =
            Map.of("terrain", List.of(DEFAULT_VARIANT, ALPHA_CUTOUT_VARIANT));

    /**
     * Defines that must be injected <b>unconditionally</b>, for every variant including
     * {@code "default"}, or the shader fails to compile at all - as opposed to
     * {@link #VARIANTS_BY_SHADER}'s optional {@code #ifdef}-gated variants, which also compile
     * fine with nothing injected. Confirmed real via a live compile failure (not guessed):
     * {@code rendertype_end_portal.fsh} references {@code PORTAL_LAYERS} with no {@code #ifdef}
     * guard at all (used to bound a loop indexing the real, confirmed 16-entry {@code COLORS[]}
     * array), so it cannot compile without this define present under any variant. {@code 16}
     * matches {@code COLORS[]}'s real array size exactly (valid indices 0-15) - the exact value
     * Mojang's {@code RenderPipeline} injects at runtime is unconfirmed, but 16 is a safe,
     * in-bounds, diagnostically meaningful choice that makes the shader compilable and testable
     * without needing to guess at exact gameplay-visual behavior, which isn't this tool's job.
     */
    private static final Map<String, String> REQUIRED_DEFINES = Map.of("rendertype_end_portal", "PORTAL_LAYERS 16");

    private static final Pattern VERSION_LINE = Pattern.compile("^(\\s*#version\\b.*)$", Pattern.MULTILINE);

    private DefineVariants() {
    }

    public static boolean isFlagged(String shaderName) {
        return FLAGGED_SHADERS.contains(shaderName);
    }

    public static List<String> variantsFor(String shaderName) {
        return VARIANTS_BY_SHADER.getOrDefault(shaderName, List.of(DEFAULT_VARIANT));
    }

    /**
     * Injects {@link #REQUIRED_DEFINES}'s entry for {@code shaderName} (if any), then - unless
     * {@code variant} is {@code "default"} - {@code #define <variant> 0.1} for the optional
     * {@code #ifdef}-gated variant. {@code 0.1} is a representative threshold value for
     * {@code ALPHA_CUTOUT} - this only needs to make the {@code #ifdef} branch compile, not
     * reproduce exact gameplay behavior.
     */
    public static String applyDefine(String source, String shaderName, String variant) {
        String result = source;

        String required = REQUIRED_DEFINES.get(shaderName);
        if (required != null) {
            result = insertDefineLine(result, required);
        }
        if (!DEFAULT_VARIANT.equals(variant)) {
            result = insertDefineLine(result, variant + " 0.1");
        }
        return result;
    }

    /** Inserts {@code #define <defineBody>} immediately after a leading {@code #version} line. */
    private static String insertDefineLine(String source, String defineBody) {
        Matcher matcher = VERSION_LINE.matcher(source);
        if (matcher.find() && matcher.start() == 0) {
            return source.substring(0, matcher.end()) + "\n#define " + defineBody + source.substring(matcher.end());
        }
        // Defensive fallback: no leading #version found where expected (shouldn't happen for a real
        // core shader), just prepend the define at the very top rather than dropping it silently.
        return "#define " + defineBody + "\n" + source;
    }
}
