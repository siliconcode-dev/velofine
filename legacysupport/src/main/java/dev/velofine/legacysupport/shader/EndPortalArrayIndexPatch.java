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

package dev.velofine.legacysupport.shader;

import dev.velofine.core.log.VelofineLog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defensive GLSL patch for a real, non-false-positive finding surfaced by tester
 * {@code diagnostic.exe} reports on the i5-3470S/HD Graphics 2500 reference machine: the real,
 * unmodified {@code rendertype_end_portal.fragment.default.glsl} (confirmed byte-identical across
 * every tester report and via javap/string search against the real 26.2 client jar this phase)
 * indexes a {@code const vec3[]} array with a genuine runtime for-loop variable:
 *
 * <pre>{@code
 * for (int i = 0; i < PORTAL_LAYERS; i++) {
 *     color += textureProj(Sampler1, texProj0 * end_portal_layer(float(i + 1))).rgb * COLORS[i];
 * }
 * }</pre>
 *
 * <p>Dynamic (non-compile-time-constant) array indexing in a fragment shader is a well-documented
 * cross-vendor GPU-driver-compiler weak point - the same category CLAUDE.md's v1.5 addendum already
 * named as a planned "general shader-robustness improvement" ("avoiding dynamic (non-constant) array
 * indexing in fragment shaders") but never implemented. {@code PORTAL_LAYERS} is a compile-time-known
 * constant ({@code #define PORTAL_LAYERS 16}, confirmed present in the real raw asset file, not
 * injected later by Mojang's own preprocessor), so a full loop unroll is mechanically safe and
 * eliminates the dynamic index entirely while preserving identical math for hardware that never had
 * the bug.
 *
 * <p>Deliberately narrow, matching {@link ShaderPatcher}'s own risk posture: this only recognizes
 * one specific shape - a simple, non-nested-brace {@code for (int VAR = 0; VAR < BOUND; VAR++) { ... }}
 * loop whose bound resolves to a literal integer - and bails out (returns the source unmodified,
 * logging why) rather than guessing at anything more general. Applied only to
 * {@code rendertype_end_portal}'s fragment shader specifically by its caller in
 * {@code LegacySupportEngine} - not a blanket transform across all shaders.
 */
public final class EndPortalArrayIndexPatch {

    // Matches "for (int VAR = 0; VAR < BOUND; VAR++) { BODY }" where BODY contains no nested braces -
    // exactly the real end_portal shader's loop shape, and narrow enough to safely bail on anything
    // more complex (nested control flow, multiple statements with their own blocks, etc.).
    private static final Pattern FOR_LOOP =
            Pattern.compile("for\\s*\\(\\s*int\\s+(\\w+)\\s*=\\s*0\\s*;\\s*\\1\\s*<\\s*(\\w+)\\s*;\\s*\\1\\+\\+\\s*\\)"
                    + "\\s*\\{([^{}]*)\\}");

    private EndPortalArrayIndexPatch() {
    }

    public static String patch(String source) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        Matcher loopMatcher = FOR_LOOP.matcher(source);
        if (!loopMatcher.find()) {
            VelofineLog.warn("LegacySupport", "end portal array-index patch: no recognizable loop shape found - "
                    + "shader source may have changed upstream, leaving vanilla source unmodified");
            return source;
        }

        String loopVar = loopMatcher.group(1);
        String boundToken = loopMatcher.group(2);
        String body = loopMatcher.group(3);

        if (!body.contains("[" + loopVar + "]")) {
            VelofineLog.warn("LegacySupport", "end portal array-index patch: matched loop does not appear to "
                    + "dynamically index an array with its own loop variable - leaving vanilla source unmodified");
            return source;
        }

        Integer bound = resolveBound(source, boundToken);
        if (bound == null) {
            VelofineLog.warn("LegacySupport", "end portal array-index patch: could not resolve loop bound \""
                    + boundToken + "\" to a compile-time constant - leaving vanilla source unmodified");
            return source;
        }

        Pattern loopVarToken = Pattern.compile("\\b" + Pattern.quote(loopVar) + "\\b");
        StringBuilder unrolled = new StringBuilder();
        for (int i = 0; i < bound; i++) {
            unrolled.append("{").append(loopVarToken.matcher(body).replaceAll(String.valueOf(i))).append("}\n");
        }

        String patched = source.substring(0, loopMatcher.start())
                + unrolled
                + source.substring(loopMatcher.end());

        VelofineLog.info("LegacySupport", "end portal array-index patch: unrolled " + bound
                + " iteration(s) of the \"" + boundToken + "\" loop, eliminating the dynamic COLORS["
                + loopVar + "] index (Intel Gen7 fragment-shader dynamic-array-index workaround)");
        return patched;
    }

    private static Integer resolveBound(String source, String token) {
        if (token.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(token);
        }
        Matcher defineMatcher = Pattern.compile("#define\\s+" + Pattern.quote(token) + "\\s+(\\d+)").matcher(source);
        if (defineMatcher.find()) {
            return Integer.parseInt(defineMatcher.group(1));
        }
        return null;
    }
}
