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

import java.util.Map;

/**
 * Result of {@code gl.UboRoundTripTester} populating one confirmed real {@code std140} uniform
 * block ({@code Fog}/{@code Globals}/{@code ChunkSection}) with sentinel values at driver-reported
 * offsets and drawing with it bound.
 *
 * <p><b>Honest scope</b>: this confirms the block populates and binds without a GL error, and that
 * the draw survives with it bound - it does <b>not</b> confirm the shader's math actually consumed
 * the uploaded values correctly. Verifying that would require mutating the shader to write an
 * observable debug value somewhere (an SSBO or a color-coded varying), which is out of scope here.
 * {@code memberOffsets} are recorded so a human can sanity-check them by hand if the round-trip
 * looks suspicious.
 */
public record UboRoundTripResult(
        String blockName,
        int bindingPoint,
        Map<String, Integer> memberOffsets,
        boolean populatedWithoutGlError,
        boolean drawSurvived) {
}
