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

package dev.velofine.diagnostics.model;

/**
 * One linked program's {@code DrawTestResult} pixel classification across two compared reports -
 * see {@code report.ReportComparator}. Deliberately a separate record from
 * {@link ShaderComparisonEntry}, not a reused/extended one: compile results are keyed per
 * (shaderName, stage, defineVariant) - two rows per shader, vertex and fragment separately - while a
 * draw test is keyed per linked <em>program</em> (shaderName, defineVariant only, no stage), so
 * folding visual classification into the same per-stage rows would either duplicate it across both
 * stages or invent a fake "stage" for it. Threshold-based, not exact-equality or perceptual
 * (SSIM-style) comparison - grounded in how real GPU conformance suites (Khronos VK-GL-CTS/dEQP)
 * compare rendered output against a reference: a fixed numeric delta, appropriate for this single-
 * pixel-sample data shape which has no anti-aliasing/structural content for a perceptual metric to
 * usefully protect against.
 */
public record VisualComparisonEntry(
        String shaderName,
        String defineVariant,
        Classification classification,
        int[] rgbaInA,
        int[] rgbaInB,
        boolean anyPixelRenderedInA,
        boolean anyPixelRenderedInB) {

    public enum Classification {
        /** Both reports' sampled pixel are within tolerance of each other. */
        UNCHANGED,
        /**
         * The sampled pixel differs beyond tolerance. When A is a known-good reference (Fix 3c's
         * bundled-baseline diff), this reads naturally as "B regressed relative to expected." When
         * comparing two arbitrary tester reports (the manual Compare screen), it just means the two
         * runs rendered visibly differently - direction is contextual, not implied by the name alone.
         */
        VISUAL_REGRESSION,
        /** Neither report rendered any pixel at all - a real finding (see DrawCallTester), not a match. */
        NOTHING_RENDERED_IN_EITHER,
        /** Present in report A only (e.g. link failed in B, or the shader is missing from B's inventory). */
        ONLY_IN_A,
        /** Present in report B only. */
        ONLY_IN_B
    }
}
