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

import dev.velofine.diagnostics.model.LintFinding;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic, best-effort static checks over shader source - flags patterns worth a human's
 * attention, not proven bugs. Two entry points, run at different pipeline stages:
 *
 * <ul>
 *   <li>{@link #lintRaw} runs on unresolved source (before {@link MojImportResolver}), tracking
 *       {@code #if}/{@code #ifdef}/{@code #endif} nesting depth specifically to flag a
 *       {@code #moj_import} line found inside an active conditional block - confirmed real in
 *       {@code entity.vsh}'s {@code #if defined(PER_FACE_LIGHTING) || !defined(NO_CARDINAL_LIGHTING)}
 *       guard around {@code #moj_import <minecraft:light.glsl>}. {@link MojImportResolver} splices
 *       unconditionally regardless of this nesting (documented there) - this is benign today (an
 *       unused-but-declared GLSL function isn't a compile error) but worth surfacing rather than
 *       leaving silently undocumented.
 *   <li>{@link #lintResolved} runs on final, post-import, post-define text: flags dynamic
 *       (non-constant) array indexing in fragment shaders, a known weak point for older Intel
 *       shader compilers (see CLAUDE.md's own "avoiding dynamic array indexing" note).
 * </ul>
 */
public final class GlslLinter {

    private static final Pattern CONDITIONAL_OPEN = Pattern.compile("^\\s*#(if|ifdef|ifndef)\\b");
    private static final Pattern CONDITIONAL_CLOSE = Pattern.compile("^\\s*#endif\\b");
    private static final Pattern MOJ_IMPORT_LINE = Pattern.compile("^\\s*#moj_import\\b");
    private static final Pattern DYNAMIC_ARRAY_INDEX = Pattern.compile("\\[\\s*([A-Za-z_]\\w*)\\s*\\]");

    private GlslLinter() {
    }

    public static List<LintFinding> lintRaw(String rawSource, String shaderName, String stage) {
        List<LintFinding> findings = new ArrayList<>();
        String[] lines = rawSource.split("\n", -1);
        int depth = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (CONDITIONAL_OPEN.matcher(line).find()) {
                depth++;
            } else if (CONDITIONAL_CLOSE.matcher(line).find()) {
                depth = Math.max(0, depth - 1);
            } else if (depth > 0 && MOJ_IMPORT_LINE.matcher(line).find()) {
                findings.add(new LintFinding(shaderName, stage, "CONDITIONAL_MOJ_IMPORT", "INFO", i + 1,
                        "#moj_import found inside an active #if/#ifdef block - the resolver splices it "
                                + "unconditionally regardless of the surrounding condition. Confirm the "
                                + "imported content being unconditionally present is harmless for this shader."));
            }
        }
        return findings;
    }

    public static List<LintFinding> lintResolved(String resolvedSource, String shaderName, String stage) {
        List<LintFinding> findings = new ArrayList<>();
        if (!"fragment".equals(stage)) {
            return findings;
        }

        String[] lines = resolvedSource.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = DYNAMIC_ARRAY_INDEX.matcher(lines[i]);
            while (matcher.find()) {
                findings.add(new LintFinding(shaderName, stage, "DYNAMIC_ARRAY_INDEX", "WARNING", i + 1,
                        "Array indexed by identifier \"" + matcher.group(1) + "\" rather than a numeric "
                                + "literal - a known weak point for older Intel fragment shader compilers. "
                                + "Heuristic match: may be a false positive if the identifier is itself a "
                                + "compile-time #define constant."));
            }
        }
        return findings;
    }
}
