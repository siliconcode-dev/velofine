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
 * {@code Masterdoc_v1.5.md} S3, plus - since v1.8-Beta - the values a real tester machine actually
 * reports, which turned out not to match the documentation (see
 * {@link #genericWmiAdapterNameOnReferenceMachineBIsStillExactVerified()}).
 */
final class LegacyGpuRegistryTest {

    @Test
    void referenceMachineAIsExactVerified() {
        GpuInfo gpu = new GpuInfo("Intel(R) HD Graphics 4000", "15.33.53.5161", GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.NONE);
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i3-3110M CPU @ 2.40GHz");

        assertEquals(GpuConfidence.EXACT_VERIFIED, LegacyGpuRegistry.classify(gpu, cpu));
    }

    @Test
    void realWmiFormattedDriverVersionForReferenceMachineAIsExactVerified() {
        // Real-world regression case: WMI (Win32_VideoController.DriverVersion, what GpuDetector
        // actually captures) reports this exact physical driver as "10.18.10.5161", not the
        // "15.33.53.5161" package-version literal Masterdoc_v1.5.md records - confirmed by a real
        // tester report from this exact reference machine. Only the trailing build segment (5161)
        // is shared between Intel's two numbering schemes.
        GpuInfo gpu = new GpuInfo("Intel(R) HD Graphics 4000", "10.18.10.5161", GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.NONE);
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i3-3110M CPU @ 2.40GHz");

        assertEquals(GpuConfidence.EXACT_VERIFIED, LegacyGpuRegistry.classify(gpu, cpu));
    }

    /**
     * The v1.8-Beta regression case, taken verbatim from a real v1.7-Beta tester log: WMI reports
     * reference machine B's adapter as the bare {@code "Intel(R) HD Graphics"} with no model number,
     * so the old model-name-only signature never matched and the machine classified as
     * {@code FAMILY_MATCH} - leaving both EXACT_VERIFIED-gated fixes silently switched off on the one
     * machine they were written for. The CPU string is the decisive signal.
     */
    @Test
    void genericWmiAdapterNameOnReferenceMachineBIsStillExactVerified() {
        GpuInfo gpu = new GpuInfo("Intel(R) HD Graphics", "10.18.10.5161", GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.NONE);
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i5-3470S CPU @ 2.90GHz");

        assertEquals(GpuConfidence.EXACT_VERIFIED, LegacyGpuRegistry.classify(gpu, cpu));
    }

    @Test
    void genericAdapterNameWithNonReferenceCpuIsOnlyFamilyMatch() {
        GpuInfo gpu = new GpuInfo("Intel(R) HD Graphics", "10.18.10.5161", GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.NONE);
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i5-3570 CPU @ 3.40GHz");

        assertEquals(GpuConfidence.FAMILY_MATCH, LegacyGpuRegistry.classify(gpu, cpu));
    }

    /**
     * Guards the Intel-adapter conjunct on the CPU-driven clause: an i5-3470S desktop with a discrete
     * card added must never have that card treated as the reference iGPU.
     */
    @Test
    void referenceCpuWithADiscreteGpuIsNotExactVerified() {
        GpuInfo gpu = new GpuInfo("NVIDIA GeForce GTX 1050 Ti", "31.0.15.3699", GpuInfo.FixProfile.NONE, GpuConfidence.NONE);
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i5-3470S CPU @ 2.90GHz");

        assertEquals(GpuConfidence.NONE, LegacyGpuRegistry.classify(gpu, cpu));
    }

    /** Mirror of the machine-B case: machine A's driver + CPU stay decisive without the model name. */
    @Test
    void genericWmiAdapterNameOnReferenceMachineAIsStillExactVerified() {
        GpuInfo gpu = new GpuInfo("Intel(R) HD Graphics", "10.18.10.5161", GpuInfo.FixProfile.INTEL_GEN7, GpuConfidence.NONE);
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
