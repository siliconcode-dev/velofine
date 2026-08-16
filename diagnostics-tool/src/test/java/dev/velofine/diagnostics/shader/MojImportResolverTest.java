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

import dev.velofine.diagnostics.shader.MojImportResolver.ImportSource;
import dev.velofine.diagnostics.shader.MojImportResolver.ResolvedShader;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

final class MojImportResolverTest {

    /** In-memory fake keyed exactly like the real jar-backed asset paths would be. */
    private static final class FakeImportSource implements ImportSource {
        final Map<String, String> angleImports = new HashMap<>();
        final Map<String, String> quotedImports = new HashMap<>();

        @Override
        public String readAngleImport(String namespace, String path) throws ImportResolutionException {
            String key = namespace + ":" + path;
            if (!angleImports.containsKey(key)) {
                throw new ImportResolutionException("no such angle import: " + key);
            }
            return angleImports.get(key);
        }

        @Override
        public String readQuotedImport(String path) throws ImportResolutionException {
            if (!quotedImports.containsKey(path)) {
                throw new ImportResolutionException("no such quoted import: " + path);
            }
            return quotedImports.get(path);
        }
    }

    @Test
    void noImportsPassesThroughUnchanged() {
        String source = "#version 330\n\nvoid main() {}\n";
        ResolvedShader resolved = MojImportResolver.resolve(source, new FakeImportSource());

        assertEquals(source, resolved.source());
        assertTrue(resolved.importChain().isEmpty());
        assertFalse(resolved.importCycleDetected());
        assertTrue(resolved.missingImports().isEmpty());
    }

    @Test
    void angleImportDefaultsToMinecraftNamespace() {
        FakeImportSource fake = new FakeImportSource();
        fake.angleImports.put("minecraft:fog.glsl", "#version 330\n\nvec4 fog() { return vec4(1.0); }\n");

        String source = "#version 330\n#moj_import <fog.glsl>\n\nvoid main() {}\n";
        ResolvedShader resolved = MojImportResolver.resolve(source, fake);

        assertTrue(resolved.source().contains("vec4 fog()"));
        // The imported file's own leading #version line must be stripped - a real GLSL compiler only
        // tolerates one #version directive, as the literal first line of the final source.
        assertFalse(resolved.source().substring(resolved.source().indexOf('\n') + 1).contains("#version"));
        assertEquals(1, resolved.importChain().size());
        assertEquals("minecraft:fog.glsl", resolved.importChain().get(0));
    }

    @Test
    void angleImportWithExplicitNamespace() {
        FakeImportSource fake = new FakeImportSource();
        fake.angleImports.put("velofine:custom.glsl", "#version 330\n\nfloat customFn() { return 1.0; }\n");

        String source = "#version 330\n#moj_import <velofine:custom.glsl>\n";
        ResolvedShader resolved = MojImportResolver.resolve(source, fake);

        assertTrue(resolved.source().contains("customFn"));
        assertEquals("velofine:custom.glsl", resolved.importChain().get(0));
    }

    @Test
    void quotedImportResolvesAgainstCoreDirectory() {
        FakeImportSource fake = new FakeImportSource();
        fake.quotedImports.put("shared_helper.glsl", "#version 330\n\nfloat helper() { return 2.0; }\n");

        String source = "#version 330\n#moj_import \"shared_helper.glsl\"\n";
        ResolvedShader resolved = MojImportResolver.resolve(source, fake);

        assertTrue(resolved.source().contains("helper()"));
        assertEquals("core:shared_helper.glsl", resolved.importChain().get(0));
    }

    @Test
    void sameImportAppearingTwiceAtTheSameLevelIsSplicedOnlyOnce() {
        // Real, confirmed bug: rendertype_end_portal.vsh imports projection.glsl TWICE (not a
        // nested self-reference, so the cycle-detection stack alone doesn't catch it).
        // projection.glsl declares a named uniform block; splicing it twice produces a real
        // 'redefinition block' GLSL compile error. Caught live by actually compiling all 34 real
        // shaders against a real GPU driver, not guessable from static inspection alone.
        FakeImportSource fake = new FakeImportSource();
        fake.angleImports.put("minecraft:projection.glsl", "#version 330\n\nuniform mat4 ProjMat;\n");

        String source = "#version 330\n#moj_import <projection.glsl>\n#moj_import <fog.glsl>\n#moj_import <projection.glsl>\n";
        fake.angleImports.put("minecraft:fog.glsl", "#version 330\n\nuniform vec4 FogColor;\n");

        ResolvedShader resolved = MojImportResolver.resolve(source, fake);

        long projMatOccurrences = resolved.source().lines().filter(l -> l.contains("uniform mat4 ProjMat")).count();
        assertEquals(1, projMatOccurrences, "projection.glsl's content must be spliced in exactly once: " + resolved.source());
        assertFalse(resolved.importCycleDetected(), "a repeated non-nested import is a dedup case, not a cycle");
        assertTrue(resolved.source().contains("already included"));
    }

    @Test
    void sameImportNestedInsideItselfIsStillACycleNotADedup() {
        // Distinguishes the new dedup logic from real cycle detection - a genuine self-referential
        // nested import must still be caught as a cycle, not silently deduped away.
        FakeImportSource fake = new FakeImportSource();
        fake.angleImports.put("minecraft:a.glsl", "#version 330\n#moj_import <a.glsl>\n");

        ResolvedShader resolved = MojImportResolver.resolve("#version 330\n#moj_import <a.glsl>\n", fake);

        assertTrue(resolved.importCycleDetected());
    }

    @Test
    void nestedImportsResolveRecursively() {
        FakeImportSource fake = new FakeImportSource();
        fake.angleImports.put("minecraft:outer.glsl", "#version 330\n#moj_import <inner.glsl>\n\nfloat outer() { return 1.0; }\n");
        fake.angleImports.put("minecraft:inner.glsl", "#version 330\n\nfloat inner() { return 2.0; }\n");

        String source = "#version 330\n#moj_import <outer.glsl>\n";
        ResolvedShader resolved = MojImportResolver.resolve(source, fake);

        assertTrue(resolved.source().contains("outer()"));
        assertTrue(resolved.source().contains("inner()"));
        assertEquals(2, resolved.importChain().size());
        assertFalse(resolved.importCycleDetected());
    }

    @Test
    void cycleIsDetectedAndDoesNotHang() {
        FakeImportSource fake = new FakeImportSource();
        fake.angleImports.put("minecraft:a.glsl", "#version 330\n#moj_import <b.glsl>\n");
        fake.angleImports.put("minecraft:b.glsl", "#version 330\n#moj_import <a.glsl>\n");

        String source = "#version 330\n#moj_import <a.glsl>\n";

        // Bounded well under any real test-runner timeout - if cycle detection is broken this call
        // recurses forever and the JVM eventually stack-overflows or hangs, so a tight assertion here
        // is deliberate, not decorative.
        ResolvedShader resolved = assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
                () -> MojImportResolver.resolve(source, fake));

        assertTrue(resolved.importCycleDetected());
    }

    @Test
    void missingImportIsRecordedNotThrown() {
        FakeImportSource fake = new FakeImportSource();
        String source = "#version 330\n#moj_import <does_not_exist.glsl>\n";

        ResolvedShader resolved = MojImportResolver.resolve(source, fake);

        assertEquals(1, resolved.missingImports().size());
        assertTrue(resolved.source().contains("moj_import not found"));
    }

    @Test
    void namespacedImportForNonVanillaNamespaceIsAMissingImportNotACrash() {
        FakeImportSource fake = new FakeImportSource();
        fake.angleImports.put("minecraft:fog.glsl", "#version 330\n\nfloat fog() { return 1.0; }\n");

        // A vanilla-only client jar (Phase 1 has no resource-pack/mod layering) can't resolve a
        // non-"minecraft" namespace - this must surface as a normal missing-import warning.
        String source = "#version 330\n#moj_import <somemod:special.glsl>\n";
        ResolvedShader resolved = MojImportResolver.resolve(source, fake);

        assertEquals(1, resolved.missingImports().size());
        assertTrue(resolved.missingImports().get(0).contains("somemod:special.glsl"));
    }

    @Test
    void splicedImportAlwaysEndsWithATrailingBlankLine() {
        FakeImportSource fake = new FakeImportSource();
        // No trailing newline at all - defends against the documented real-loader quirk where a
        // missing trailing blank line breaks resolution.
        fake.angleImports.put("minecraft:no_trailing_newline.glsl", "#version 330\nfloat x() { return 1.0; }");

        String source = "#version 330\n#moj_import <no_trailing_newline.glsl>\nvoid main() {}\n";
        ResolvedShader resolved = MojImportResolver.resolve(source, fake);

        int spliceEnd = resolved.source().indexOf("void main()");
        String splicedRegion = resolved.source().substring(0, spliceEnd);
        assertTrue(splicedRegion.endsWith("\n\n"), "spliced content must end with a genuine blank line: [" + splicedRegion + "]");
    }
}
