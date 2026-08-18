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
 * Result of a real {@code glDrawArrays} call against a synthesized minimal VAO/VBO, produced by
 * {@code gl.DrawCallTester}.
 *
 * <p><b>{@code drawSucceededNoNewError} means the draw call completed without a new GL error and
 * the framebuffer stayed complete</b> - a crash/error/hang check, not by itself a visual-
 * correctness check. It's still not real in-game rendering (a single point primitive, not real
 * game geometry), but as of Fix 3 (v1.5 diagnostic-tool rework) it's no longer structurally blind
 * to visual output: the framebuffer is cleared to a fixed sentinel color before the draw, real
 * per-shader uniform defaults (identity matrices, non-zero scalars) and real extracted textures are
 * bound (see {@code gl.UniformDefaults}/{@code gl.TextureBinder}), and the readback scans the whole
 * buffer for any pixel that differs from the sentinel rather than assuming the center pixel is where
 * geometry landed. {@code anyPixelRendered}/{@code sampledRgba} are the structured, programmatically
 * diffable result ({@code report.ReportComparator} uses these for golden-reference visual-regression
 * comparison, Fix 3c); {@code pixelSampleSummary} is the same information as a human-readable string.
 */
public record DrawTestResult(
        boolean framebufferCompleteBeforeDraw,
        boolean drawSucceededNoNewError,
        boolean anyPixelRendered,
        int[] sampledRgba,
        String pixelSampleSummary,
        String screenshotRelativePath) {
}
