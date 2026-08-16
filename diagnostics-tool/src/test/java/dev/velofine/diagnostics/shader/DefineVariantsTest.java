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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefineVariantsTest {

    @Test
    void terrainHasDefaultAndAlphaCutoutVariants() {
        assertEquals(List.of("default", "ALPHA_CUTOUT"), DefineVariants.variantsFor("terrain"));
    }

    @Test
    void unflaggedShaderHasOnlyDefaultVariant() {
        assertEquals(List.of("default"), DefineVariants.variantsFor("gui"));
    }

    @Test
    void alphaCutoutInjectsDefineOnlyForThatVariant() {
        String source = "#version 330\nvoid main() {}\n";

        String withoutCutout = DefineVariants.applyDefine(source, "terrain", "default");
        assertFalse(withoutCutout.contains("#define ALPHA_CUTOUT"));

        String withCutout = DefineVariants.applyDefine(source, "terrain", "ALPHA_CUTOUT");
        assertTrue(withCutout.contains("#define ALPHA_CUTOUT 0.1"));
    }

    @Test
    void portalLayersIsInjectedUnconditionallyEvenForDefaultVariant() {
        // Real, confirmed via a live compile failure: rendertype_end_portal.fsh has no #ifdef
        // guard around PORTAL_LAYERS at all - it must be present for every variant, not just a
        // named alternate one, unlike terrain's ALPHA_CUTOUT.
        String source = "#version 330\nvoid main() {}\n";
        String defined = DefineVariants.applyDefine(source, "rendertype_end_portal", "default");

        assertTrue(defined.contains("#define PORTAL_LAYERS 16"));
    }

    @Test
    void requiredDefineComesBeforeVariantDefineButBothPresent() {
        // Not a real case today (rendertype_end_portal has no #ifdef-gated variant), but confirms
        // the two injection mechanisms don't clobber each other if a shader ever needs both.
        String source = "#version 330\nvoid main() {}\n";
        String defined = DefineVariants.applyDefine(source, "rendertype_end_portal", "SOME_VARIANT");

        assertTrue(defined.contains("#define PORTAL_LAYERS 16"));
        assertTrue(defined.contains("#define SOME_VARIANT 0.1"));
        long versionCount = defined.lines().filter(l -> l.strip().startsWith("#version")).count();
        assertEquals(1, versionCount);
    }

    @Test
    void unrelatedShaderGetsNoRequiredDefine() {
        String source = "#version 330\nvoid main() {}\n";
        String defined = DefineVariants.applyDefine(source, "terrain", "default");
        assertFalse(defined.contains("PORTAL_LAYERS"));
    }
}
