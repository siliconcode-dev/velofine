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

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link EndPortalArrayIndexPatch} against the real, unmodified
 * {@code rendertype_end_portal.fragment.default.glsl} text (confirmed byte-identical across every
 * tester {@code diagnostic.exe} report and against the real 26.2 client jar this phase) - a pure text
 * transform, so this is directly and cheaply unit-testable without a real GL context, unlike
 * {@link EndPortalArrayIndexPatch}'s effect on actual rendering.
 */
final class EndPortalArrayIndexPatchTest {

    /**
     * The <b>raw asset</b> as it really ships in the jar - note there is no {@code #define
     * PORTAL_LAYERS}, because that value is a {@code ShaderDefines} entry on
     * {@code RenderPipelines.END_PORTAL} and is spliced in later by
     * {@code GlslPreprocessor.injectDefines}. v1.7-Beta's fixture silently included the {@code
     * #define}, so its unroll test passed while the shipped code could never work - see
     * {@link #rawAssetSourceWithoutInjectedDefinesBailsOutSafely()}.
     */
    private static final String RAW_END_PORTAL_FRAGMENT = """
            #version 330

            uniform sampler2D Sampler0;
            uniform sampler2D Sampler1;

            in vec4 texProj0;

            const vec3[] COLORS = vec3[](
                vec3(0.022087, 0.098399, 0.110818),
                vec3(0.011892, 0.095924, 0.089485)
            );

            mat4 end_portal_layer(float layer) {
                return mat4(1.0);
            }

            out vec4 fragColor;

            void main() {
                vec3 color = textureProj(Sampler0, texProj0).rgb * COLORS[0];
                for (int i = 0; i < PORTAL_LAYERS; i++) {
                    color += textureProj(Sampler1, texProj0 * end_portal_layer(float(i + 1))).rgb * COLORS[i];
                }
                fragColor = vec4(color, 1.0);
            }
            """;

    /**
     * What the patch actually receives at stage 2: the raw asset after
     * {@code GlslPreprocessor.injectDefines} has spliced the pipeline's defines in after the
     * {@code #version} line.
     */
    private static final String POST_DEFINES_END_PORTAL_FRAGMENT =
            RAW_END_PORTAL_FRAGMENT.replace("#version 330\n", "#version 330\n#define PORTAL_LAYERS 16\n");

    @Test
    void unrollsTheRealEndPortalLoop() {
        String patched = EndPortalArrayIndexPatch.patch(POST_DEFINES_END_PORTAL_FRAGMENT);

        assertFalse(patched.contains("COLORS[i]"), "dynamic index should be gone");
        assertFalse(patched.contains("for (int i"), "the for-loop construct should be gone");
        assertTrue(patched.contains("COLORS[15]"), "the last unrolled index should be present");
        assertTrue(patched.contains("COLORS[0]"), "the first unrolled index (of the loop body) should be present");
        assertEquals(16, countOccurrences(patched, "textureProj(Sampler1,"),
                "one textureProj(Sampler1, ...) call per unrolled iteration");
        assertEquals(countChar(patched, '{'), countChar(patched, '}'), "braces must stay balanced");
    }

    /**
     * Reproduces the exact v1.7-Beta field failure. Registered at the pre-{@code #define} stage, the
     * patch was handed this raw text on every launch, could not resolve {@code PORTAL_LAYERS}, and
     * bailed out - so the shipped "fix" never once modified a shader on the tester's hardware
     * ("could not resolve loop bound \"PORTAL_LAYERS\"" in the real log). Bailing out is the correct
     * behavior for this input; the actual fix was moving the registration to stage 2, which is what
     * {@link #unrollsTheRealEndPortalLoop()} now covers. Keep both: together they pin down that the
     * patch is correct <em>and</em> that it is being fed the right text.
     */
    @Test
    void rawAssetSourceWithoutInjectedDefinesBailsOutSafely() {
        assertSame(RAW_END_PORTAL_FRAGMENT, EndPortalArrayIndexPatch.patch(RAW_END_PORTAL_FRAGMENT),
                "raw asset text has no #define PORTAL_LAYERS - the bound is unresolvable and the patch "
                        + "must leave the source alone rather than guess");
    }

    @Test
    void leavesSourceWithNoRecognizedLoopUnchanged() {
        String source = "void main() { fragColor = vec4(1.0); }";
        assertSame(source, EndPortalArrayIndexPatch.patch(source), "no matching loop shape - must bail out safely");
    }

    @Test
    void leavesSourceWithUnresolvableBoundUnchanged() {
        String source = """
                void main() {
                    for (int i = 0; i < UNKNOWN_BOUND; i++) {
                        color += COLORS[i];
                    }
                }
                """;
        assertSame(source, EndPortalArrayIndexPatch.patch(source),
                "loop bound doesn't resolve to a literal - must bail out safely rather than guess");
    }

    @Test
    void leavesSourceWhereLoopVariableIsNotUsedAsAnArrayIndexUnchanged() {
        String source = """
                #define COUNT 4
                void main() {
                    for (int i = 0; i < COUNT; i++) {
                        total += 1.0;
                    }
                }
                """;
        assertSame(source, EndPortalArrayIndexPatch.patch(source),
                "loop variable isn't used as an array index - not the pattern this patch targets");
    }

    @Test
    void nullAndEmptySourceArePassedThroughUnchanged() {
        assertSame(null, EndPortalArrayIndexPatch.patch(null));
        assertEquals("", EndPortalArrayIndexPatch.patch(""));
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        Matcher matcher = Pattern.compile(Pattern.quote(needle)).matcher(haystack);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static long countChar(String s, char c) {
        return s.chars().filter(ch -> ch == c).count();
    }
}
