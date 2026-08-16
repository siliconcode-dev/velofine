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
 * <p><b>{@code drawSucceededNoNewError} means exactly one thing: the draw call completed without
 * a new GL error and the framebuffer stayed complete.</b> It does <b>not</b> mean "renders
 * correctly in-game" - the synthesized geometry is a single zero-filled point, not real game data,
 * and no textures are bound. A passing tile here rules out a driver-level crash/error/hang on this
 * shader's draw path; it says nothing about visual correctness. Read {@code pixelSampleSummary}
 * as "what value did the framebuffer end up with," not "is this the right color."
 */
public record DrawTestResult(
        boolean framebufferCompleteBeforeDraw,
        boolean drawSucceededNoNewError,
        String pixelSampleSummary,
        String screenshotRelativePath) {
}
