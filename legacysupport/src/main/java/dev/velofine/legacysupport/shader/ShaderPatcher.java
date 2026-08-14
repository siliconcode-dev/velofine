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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Defensive GLSL patch for a known Intel Gen7 shader-compiler bug (Intel Community forum, via
 * this phase's research): {@code mix(a, b, <literal>)} with a compile-time-constant blend factor
 * triggers broken constant folding in Intel's driver, producing NaN/wrong output. The confirmed
 * workaround is wrapping the literal in a no-op {@code clamp01()} call, which defeats the buggy
 * optimization path.
 *
 * <p>We don't know which specific shader(s) in Minecraft 26.2 trigger this - Masterdoc frames it
 * as "informed by prior art" (the same bug <em>class</em>), not a pinpointed line - so this is
 * applied to every {@code mix()} call with a literal third argument found in any shader source
 * loaded while the Intel Gen7 fix profile is active, logged per-shader so it's falsifiable rather
 * than a silent blanket change.
 */
public final class ShaderPatcher {

    // Captures mix(<arg1>, <arg2>, <literal>) allowing one level of nested parens in arg1/arg2
    // (e.g. vec3(...) arguments); the third argument must be a plain numeric literal - not an
    // identifier/expression - matching exactly the constant-folding bug's trigger condition.
    private static final Pattern MIX_WITH_LITERAL_BLEND =
            Pattern.compile("\\bmix\\(((?:[^()]|\\([^()]*\\))*),\\s*((?:[^()]|\\([^()]*\\))*),\\s*(-?\\d+\\.?\\d*)\\s*\\)");

    private static final String CLAMP01_HELPER = "float clamp01(float x) { return clamp(x, 0.0, 1.0); }\n";

    private ShaderPatcher() {
    }

    public static String patch(String source, String label) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        Matcher matcher = MIX_WITH_LITERAL_BLEND.matcher(source);
        StringBuilder rewritten = new StringBuilder();
        int patchedCalls = 0;
        while (matcher.find()) {
            patchedCalls++;
            String replacement = "mix(" + matcher.group(1) + ", " + matcher.group(2) + ", clamp01(" + matcher.group(3) + "))";
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(rewritten);

        if (patchedCalls == 0) {
            return source;
        }

        String patched = rewritten.toString();
        if (!patched.contains("float clamp01(")) {
            patched = insertAfterVersionDirective(patched, CLAMP01_HELPER);
        }

        System.out.println("[Velofine][LegacySupport] shader patch: " + label + " - wrapped " + patchedCalls
                + " mix() call(s) with literal blend factors in clamp01() (Intel constant-folding workaround)");
        return patched;
    }

    private static String insertAfterVersionDirective(String source, String toInsert) {
        int versionIdx = source.indexOf("#version");
        if (versionIdx < 0) {
            return toInsert + source;
        }
        int lineEnd = source.indexOf('\n', versionIdx);
        if (lineEnd < 0) {
            return source + "\n" + toInsert;
        }
        return source.substring(0, lineEnd + 1) + toInsert + source.substring(lineEnd + 1);
    }
}
