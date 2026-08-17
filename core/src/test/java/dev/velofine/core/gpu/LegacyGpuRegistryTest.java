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
 * Validates {@link LegacyGpuRegistry#classify} against the literal reference-machine values from
 * {@code Masterdoc_v1.5.md} S3, since no real tester-submitted diagnostic report has landed in
 * {@code testers-diagnosis-report/} yet to validate against directly (see this phase's plan notes
 * in {@code Claude_v1.5.md}).
 */
final class LegacyGpuRegistryTest {

    @Test
    void referenceMachineAIsExactVerified() {
        GpuInfo gpu = new GpuInfo("Intel(R) HD Graphics 4000", "15.33.53.5161", GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.NONE);
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i3-3110M CPU @ 2.40GHz");

        assertEquals(GpuConfidence.EXACT_VERIFIED, LegacyGpuRegistry.classify(gpu, cpu));
    }

    @Test
    void sameGpuAndDriverButDifferentCpuIsOnlyFamilyMatch() {
        // A different Ivy Bridge laptop with the identical GPU+driver but not the exact reference
        // CPU - Build_plan Phase 2's confidence tiering exists precisely for this case.
        GpuInfo gpu = new GpuInfo("Intel(R) HD Graphics 4000", "15.33.53.5161", GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.NONE);
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i7-3520M CPU @ 2.90GHz");

        assertEquals(GpuConfidence.FAMILY_MATCH, LegacyGpuRegistry.classify(gpu, cpu));
    }

    @Test
    void sameCpuAndGpuButDifferentDriverIsOnlyFamilyMatch() {
        GpuInfo gpu = new GpuInfo("Intel(R) HD Graphics 4000", "9.17.10.4459", GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.NONE);
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i3-3110M CPU @ 2.40GHz");

        assertEquals(GpuConfidence.FAMILY_MATCH, LegacyGpuRegistry.classify(gpu, cpu));
    }

    @Test
    void referenceMachineBIsExactVerifiedRegardlessOfCpu() {
        // Machine B's driver is Windows-provided with no fixed version string to check against -
        // GPU model alone is the signature (Masterdoc_v1.5.md S3), CPU model is not required.
        GpuInfo gpu = new GpuInfo("Intel(R) HD Graphics 2500", "10.18.10.3958", GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.NONE);
        CpuInfo cpu = CpuInfo.unknown();

        assertEquals(GpuConfidence.EXACT_VERIFIED, LegacyGpuRegistry.classify(gpu, cpu));
    }

    @Test
    void broaderIntelHdGraphicsFamilyIsFamilyMatchOnly() {
        GpuInfo gpu = new GpuInfo("Intel(R) HD Graphics 3000", null, GpuInfo.FixProfile.GENERIC_OLD, GpuConfidence.NONE);

        assertEquals(GpuConfidence.FAMILY_MATCH, LegacyGpuRegistry.classify(gpu, CpuInfo.unknown()));
    }

    @Test
    void modernGpuIsNone() {
        GpuInfo gpu = new GpuInfo("NVIDIA GeForce RTX 4070", "551.23", GpuInfo.FixProfile.NONE, GpuConfidence.NONE);

        assertEquals(GpuConfidence.NONE, LegacyGpuRegistry.classify(gpu, CpuInfo.unknown()));
    }

    @Test
    void unknownGpuIsNone() {
        assertEquals(GpuConfidence.NONE, LegacyGpuRegistry.classify(GpuInfo.unknown(), CpuInfo.unknown()));
    }
}
