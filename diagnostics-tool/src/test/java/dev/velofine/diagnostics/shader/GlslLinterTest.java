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
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GlslLinterTest {

    @Test
    void flagsMojImportInsideActiveConditionalBlock() {
        // Real structure confirmed in entity.vsh.
        String source = "#version 330\n"
                + "#if defined(PER_FACE_LIGHTING) || !defined(NO_CARDINAL_LIGHTING)\n"
                + "#moj_import <minecraft:light.glsl>\n"
                + "#endif\n"
                + "#moj_import <minecraft:fog.glsl>\n";

        List<LintFinding> findings = GlslLinter.lintRaw(source, "entity", "vertex");

        assertEquals(1, findings.size());
        assertEquals("CONDITIONAL_MOJ_IMPORT", findings.get(0).ruleId());
        assertEquals(3, findings.get(0).line());
    }

    @Test
    void doesNotFlagMojImportOutsideAnyConditional() {
        String source = "#version 330\n#moj_import <minecraft:fog.glsl>\n";
        assertTrue(GlslLinter.lintRaw(source, "terrain", "fragment").isEmpty());
    }

    @Test
    void nestedConditionalsAreTrackedCorrectly() {
        String source = "#version 330\n"
                + "#ifdef A\n"
                + "#ifdef B\n"
                + "#moj_import <minecraft:x.glsl>\n"
                + "#endif\n"
                + "#endif\n"
                + "#moj_import <minecraft:y.glsl>\n"; // outside both, at depth 0

        List<LintFinding> findings = GlslLinter.lintRaw(source, "x", "fragment");
        assertEquals(1, findings.size());
        assertEquals(4, findings.get(0).line());
    }

    @Test
    void flagsDynamicArrayIndexingInFragmentShaders() {
        String source = "#version 330\nvoid main() { color = COLORS[i]; }\n";
        List<LintFinding> findings = GlslLinter.lintResolved(source, "rendertype_end_portal", "fragment");

        assertEquals(1, findings.size());
        assertEquals("DYNAMIC_ARRAY_INDEX", findings.get(0).ruleId());
    }

    @Test
    void doesNotFlagConstantArrayIndexing() {
        String source = "#version 330\nvoid main() { color = COLORS[0]; }\n";
        assertTrue(GlslLinter.lintResolved(source, "rendertype_end_portal", "fragment").isEmpty());
    }

    @Test
    void vertexStageIsNeverLintedForDynamicArrayIndexing() {
        // Scope decision: dynamic-array-indexing risk is specifically fragment-shader-relevant per
        // CLAUDE.md's own note - lintResolved should not flag vertex stages at all.
        String source = "#version 330\nvoid main() { color = COLORS[i]; }\n";
        assertTrue(GlslLinter.lintResolved(source, "terrain", "vertex").isEmpty());
    }
}
