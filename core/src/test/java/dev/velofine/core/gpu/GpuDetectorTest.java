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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers {@link GpuDetector#classifyFixProfile}, extracted from the PowerShell-invoking
 * {@code detect()} in v1.8-Beta specifically so this logic could be tested at all.
 *
 * <p>The motivating case is {@link #genericIntelAdapterOnIvyBridgeCpuIsGen7()}: WMI reported the real
 * reference machine's iGPU as a bare {@code "Intel(R) HD Graphics"}, which fell through the
 * model-number pattern to {@code GENERIC_OLD} - and {@code GENERIC_OLD} grants only
 * {@code GL_COMPATIBILITY_PROFILE}, so the machine silently lost {@code SHADER_MIX_PATCH} as well as
 * both v1.7 fixes.
 */
final class GpuDetectorTest {

    private static final CpuInfo IVY_BRIDGE = new CpuInfo("Intel(R) Core(TM) i5-3470S CPU @ 2.90GHz");
    private static final CpuInfo MODERN = new CpuInfo("Intel(R) Core(TM) i5-8265U CPU @ 1.60GHz");

    @Test
    void explicitGen7ModelNamesAreGen7RegardlessOfCpu() {
        assertEquals(GpuInfo.FixProfile.INTEL_GEN7,
                GpuDetector.classifyFixProfile("Intel(R) HD Graphics 4000", MODERN));
        assertEquals(GpuInfo.FixProfile.INTEL_GEN7,
                GpuDetector.classifyFixProfile("Intel(R) HD Graphics 2500", CpuInfo.unknown()));
    }

    @Test
    void genericIntelAdapterOnIvyBridgeCpuIsGen7() {
        assertEquals(GpuInfo.FixProfile.INTEL_GEN7,
                GpuDetector.classifyFixProfile("Intel(R) HD Graphics", IVY_BRIDGE));
    }

    @Test
    void genericIntelAdapterOnNonIvyBridgeCpuStaysGenericOld() {
        assertEquals(GpuInfo.FixProfile.GENERIC_OLD,
                GpuDetector.classifyFixProfile("Intel(R) HD Graphics", MODERN));
        assertEquals(GpuInfo.FixProfile.GENERIC_OLD,
                GpuDetector.classifyFixProfile("Intel(R) HD Graphics", CpuInfo.unknown()));
    }

    /**
     * Documents pre-existing v1 behavior, unchanged here: "UHD Graphics 620" still contains the
     * "HD Graphics" token, so it lands in the broad GENERIC_OLD safety net rather than NONE. That net
     * only grants the conservative GL_COMPATIBILITY_PROFILE, and the v1.8-Beta CPU clause does not
     * widen it - a modern CPU keeps such an adapter out of INTEL_GEN7.
     */
    @Test
    void newerIntelBrandingStaysInTheBroadGenericOldNetNotGen7() {
        assertEquals(GpuInfo.FixProfile.GENERIC_OLD,
                GpuDetector.classifyFixProfile("Intel(R) UHD Graphics 620", MODERN));
    }

    @Test
    void nonIntelAdaptersAreNone() {
        assertEquals(GpuInfo.FixProfile.NONE,
                GpuDetector.classifyFixProfile("NVIDIA GeForce GTX 1050 Ti", IVY_BRIDGE));
        assertEquals(GpuInfo.FixProfile.NONE, GpuDetector.classifyFixProfile(null, IVY_BRIDGE));
    }
}
