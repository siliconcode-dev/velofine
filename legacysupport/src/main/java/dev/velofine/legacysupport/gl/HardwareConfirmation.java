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

package dev.velofine.legacysupport.gl;

import dev.velofine.core.gpu.AdapterMatcher;
import dev.velofine.core.hardware.HardwareProfile;

import java.util.Optional;

/**
 * The actual gate {@code Masterdoc_v1.5.md} S4 asks for: "post-context {@code glGetString}
 * confirmation... as the actual gate before any shader swap is applied." Protects against
 * hybrid/switchable-graphics laptops where pre-context WMI detection and the GL context that
 * actually got created disagree (WMI lists an Intel iGPU while rendering actually bound to a
 * discrete NVIDIA/AMD part, or vice versa).
 */
public final class HardwareConfirmation {

    private HardwareConfirmation() {
    }

    /**
     * True only if the live {@code GL_RENDERER} correlates with the WMI-detected adapter the
     * pre-context fix decision was based on. False (safe default - no shader swap) if either side
     * is unavailable, matching {@code GpuDetector}/{@code GlContextSignature}'s own
     * catch-and-degrade style.
     */
    public static boolean isConfirmedMatch(HardwareProfile profile) {
        Optional<GlContextSignature.Signature> signature = GlContextSignature.get();
        if (signature.isEmpty()) {
            return false;
        }
        return AdapterMatcher.matches(signature.get().renderer(), profile.gpu().adapterName());
    }
}
