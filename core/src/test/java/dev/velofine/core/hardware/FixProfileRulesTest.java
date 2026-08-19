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

package dev.velofine.core.hardware;

import dev.velofine.core.gpu.CpuInfo;
import dev.velofine.core.gpu.GpuConfidence;
import dev.velofine.core.gpu.GpuInfo;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FixProfileRules#resolve(HardwareProfile)} is a genuinely pure function over a fixed list
 * of independent, additive rules - no hardware detection, no I/O, no static state - so real
 * combinations can be asserted exactly, including the "a machine can match several rules at once"
 * case the class's own javadoc calls out (Build_plan Phase 3's motivating scenario).
 */
final class FixProfileRulesTest {

    @Test
    void unknownHardwareActivatesNoFixes() {
        assertEquals(Set.of(), FixProfileRules.resolve(HardwareProfile.unknown()));
    }

    @Test
    void intelGen7GpuActivatesGlCompatibilityAndShaderMixPatch() {
        HardwareProfile profile = new HardwareProfile(CpuInfo.unknown(),
                new GpuInfo("Intel(R) HD Graphics 4000", "15.33.53.5161", GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.NONE),
                MemoryInfo.unknown(), DiskInfo.unknown());

        assertEquals(Set.of(Fix.GL_COMPATIBILITY_PROFILE, Fix.SHADER_MIX_PATCH), FixProfileRules.resolve(profile));
    }

    @Test
    void genericOldGpuActivatesOnlyGlCompatibility() {
        HardwareProfile profile = new HardwareProfile(CpuInfo.unknown(),
                new GpuInfo("Some Ancient Adapter", "1.0.0", GpuInfo.FixProfile.GENERIC_OLD, GpuConfidence.NONE),
                MemoryInfo.unknown(), DiskInfo.unknown());

        assertEquals(Set.of(Fix.GL_COMPATIBILITY_PROFILE), FixProfileRules.resolve(profile));
    }

    @Test
    void rotationalDiskActivatesIoStallSmoothingOnly() {
        HardwareProfile profile = new HardwareProfile(CpuInfo.unknown(), GpuInfo.unknown(), MemoryInfo.unknown(), new DiskInfo(true));

        assertEquals(Set.of(Fix.IO_STALL_SMOOTHING), FixProfileRules.resolve(profile));
    }

    @Test
    void lowMemoryActivatesMemorySavingDefaultsOnly() {
        HardwareProfile profile = new HardwareProfile(CpuInfo.unknown(), GpuInfo.unknown(), new MemoryInfo(4L * 1024 * 1024 * 1024), DiskInfo.unknown());

        assertEquals(Set.of(Fix.MEMORY_SAVING_DEFAULTS), FixProfileRules.resolve(profile));
    }

    @Test
    void memoryAboveTheCeilingIsNotConsideredLow() {
        HardwareProfile profile = new HardwareProfile(CpuInfo.unknown(), GpuInfo.unknown(), new MemoryInfo(8L * 1024 * 1024 * 1024), DiskInfo.unknown());

        assertEquals(Set.of(), FixProfileRules.resolve(profile));
    }

    @Test
    void rulesAreAdditiveAcrossIndependentHardwareCharacteristics() {
        // The i3-3110M reference laptop: Intel Gen7 + rotational HDD + 4GB RAM - matches four
        // rules at once (three hardware-characteristic rules plus the EXACT_VERIFIED-gated one,
        // since this profile is the real reference machine), exactly the scenario
        // FixProfileRules' class javadoc is written around.
        HardwareProfile profile = new HardwareProfile(
                new CpuInfo("Intel(R) Core(TM) i3-3110M CPU @ 2.40GHz"),
                new GpuInfo("Intel(R) HD Graphics 4000", "15.33.53.5161", GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.EXACT_VERIFIED),
                new MemoryInfo(4L * 1024 * 1024 * 1024),
                new DiskInfo(true));

        Set<Fix> resolved = FixProfileRules.resolve(profile);

        assertEquals(Set.of(Fix.GL_COMPATIBILITY_PROFILE, Fix.SHADER_MIX_PATCH,
                Fix.IO_STALL_SMOOTHING, Fix.MEMORY_SAVING_DEFAULTS, Fix.ANIMATED_TEXTURE_UPLOAD_FIX,
                Fix.END_PORTAL_ARRAY_INDEX_PATCH), resolved);
        assertTrue(resolved.size() == 6);
    }

    @Test
    void exactVerifiedConfidenceAlsoActivatesAnimatedTextureUploadFix() {
        HardwareProfile profile = new HardwareProfile(CpuInfo.unknown(),
                new GpuInfo("Intel(R) HD Graphics 2500", null, GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.EXACT_VERIFIED),
                MemoryInfo.unknown(), DiskInfo.unknown());

        assertEquals(Set.of(Fix.GL_COMPATIBILITY_PROFILE, Fix.SHADER_MIX_PATCH, Fix.ANIMATED_TEXTURE_UPLOAD_FIX,
                Fix.END_PORTAL_ARRAY_INDEX_PATCH),
                FixProfileRules.resolve(profile));
    }

    @Test
    void familyMatchConfidenceDoesNotActivateAnimatedTextureUploadFix() {
        // A broader, unverified Intel Gen7-family match should stay on the conservative fixes only -
        // v1.6-Beta's fix is categorically more invasive and is deliberately not extended to this tier.
        HardwareProfile profile = new HardwareProfile(CpuInfo.unknown(),
                new GpuInfo("Intel(R) HD Graphics", null, GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.FAMILY_MATCH),
                MemoryInfo.unknown(), DiskInfo.unknown());

        assertEquals(Set.of(Fix.GL_COMPATIBILITY_PROFILE, Fix.SHADER_MIX_PATCH), FixProfileRules.resolve(profile));
    }
}
