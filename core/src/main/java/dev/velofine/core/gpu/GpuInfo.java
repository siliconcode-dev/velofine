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

package dev.velofine.core.gpu;

/**
 * Detected graphics adapter identity plus the LegacySupport fix profile it maps to.
 *
 * @param adapterName   raw adapter name as reported by Windows (e.g. "Intel(R) HD Graphics 4000"),
 *                      or {@code null} if detection failed/found nothing.
 * @param driverVersion raw Windows driver version string (e.g. "15.33.53.5161"), or {@code null}.
 * @param fixProfile    which LegacySupport fix profile applies to this hardware.
 * @param confidence    v1.5: how confidently this matches one of the two personally-verified
 *                      reference machines vs. just the broader family - see {@link GpuConfidence}
 *                      and {@link LegacyGpuRegistry}. Independent of {@code fixProfile}: it
 *                      refines the same detection with an extra confidence signal rather than
 *                      replacing it, so v1's {@code fixProfile}-driven fixes are unaffected.
 */
public record GpuInfo(String adapterName, String driverVersion, FixProfile fixProfile, GpuConfidence confidence) {

    /**
     * Ivy Bridge Gen7 Intel iGPUs (HD Graphics 4000/2500) — the confirmed, personally-verified
     * bug: forces a compatibility-profile GL context and applies the defensive shader mix()
     * patch. See Masterdoc.md and CLAUDE.md's "Mixin tooling decision".
     */
    public enum FixProfile {
        INTEL_GEN7,
        GENERIC_OLD,
        NONE
    }

    public static GpuInfo unknown() {
        return new GpuInfo(null, null, FixProfile.NONE, GpuConfidence.NONE);
    }
}
