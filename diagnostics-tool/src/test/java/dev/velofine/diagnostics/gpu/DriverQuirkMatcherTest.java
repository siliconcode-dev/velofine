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

package dev.velofine.diagnostics.gpu;

import dev.velofine.diagnostics.model.CpuInfo;
import dev.velofine.diagnostics.model.GpuInfo;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DriverQuirkMatcherTest {

    @Test
    void exactMatchOnReferenceMachineA() {
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i3-3110M CPU @ 2.40GHz", 2, 4);
        List<GpuInfo> gpus = List.of(new GpuInfo("Intel(R) HD Graphics 4000", "15.33.53.5161", null, null, null));

        List<String> notes = DriverQuirkMatcher.match(cpu, gpus);

        assertEquals(1, notes.size());
        assertTrue(notes.get(0).contains("reference machine A"));
        assertTrue(notes.get(0).contains("targeted shader-patch fix"));
    }

    @Test
    void exactMatchOnReferenceMachineAWithRealWmiDriverFormat() {
        // Real-world regression case: WMI reports this exact physical driver as "10.18.10.5161",
        // not the "15.33.53.5161" package-version literal - confirmed by a real tester report from
        // reference machine A itself. Only the trailing build segment (5161) is shared between
        // Intel's two numbering schemes.
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i3-3110M CPU @ 2.40GHz", 2, 4);
        List<GpuInfo> gpus = List.of(new GpuInfo("Intel(R) HD Graphics 4000", "10.18.10.5161", null, null, null));

        List<String> notes = DriverQuirkMatcher.match(cpu, gpus);

        assertEquals(1, notes.size());
        assertTrue(notes.get(0).contains("reference machine A"));
        assertTrue(notes.get(0).contains("targeted shader-patch fix"));
    }

    @Test
    void gpuOnlyMatchOnReferenceMachineBGpuWithoutConfirmedCpu() {
        CpuInfo cpu = new CpuInfo("Some Other CPU", 4, 8);
        List<GpuInfo> gpus = List.of(new GpuInfo("Intel(R) HD Graphics 2500", "unknown", null, null, null));

        List<String> notes = DriverQuirkMatcher.match(cpu, gpus);

        assertEquals(1, notes.size());
        assertTrue(notes.get(0).contains("reference machine B"));
        assertTrue(notes.get(0).contains("was not confirmed"));
    }

    /**
     * v1.8-Beta, mirroring {@code core.gpu.LegacyGpuRegistryTest}: WMI reports reference machine B's
     * adapter as a bare "Intel(R) HD Graphics" with no model number (confirmed in a real tester log),
     * so the CPU model has to carry the identification. Kept in lockstep with core deliberately - the
     * point of this class is that a tester reading "matches reference machine B" in a report sees the
     * same verdict the live agent reached.
     */
    @Test
    void genericAdapterNameWithReferenceCpuStillMatchesMachineB() {
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i5-3470S CPU @ 2.90GHz", 4, 4);
        List<GpuInfo> gpus = List.of(new GpuInfo("Intel(R) HD Graphics", "10.18.10.5161", null, null, null));

        List<String> notes = DriverQuirkMatcher.match(cpu, gpus);

        assertEquals(1, notes.size());
        assertTrue(notes.get(0).contains("reference machine B"));
        assertTrue(notes.get(0).contains("Identified by CPU model"));
    }

    /** The Intel-adapter conjunct: a discrete card in a reference-CPU desktop must not match. */
    @Test
    void referenceCpuWithDiscreteGpuDoesNotMatchMachineB() {
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i5-3470S CPU @ 2.90GHz", 4, 4);
        List<GpuInfo> gpus = List.of(new GpuInfo("NVIDIA GeForce GTX 1050 Ti", "31.0.15.3699", null, null, null));

        assertTrue(DriverQuirkMatcher.match(cpu, gpus).isEmpty());
    }

    @Test
    void broaderFamilyMatchWhenDriverVersionDiffersFromExactReference() {
        CpuInfo cpu = new CpuInfo("Intel(R) Core(TM) i3-3110M CPU @ 2.40GHz", 2, 4);
        // Same GPU name as machine A but a different driver version - not an exact match.
        List<GpuInfo> gpus = List.of(new GpuInfo("Intel(R) HD Graphics 4000", "20.19.15.4531", null, null, null));

        List<String> notes = DriverQuirkMatcher.match(cpu, gpus);

        assertEquals(1, notes.size());
        assertTrue(notes.get(0).contains("compatibility-renderer fallback"));
    }

    @Test
    void noNotesForUnrelatedHardware() {
        CpuInfo cpu = new CpuInfo("AMD Ryzen 7", 8, 16);
        List<GpuInfo> gpus = List.of(new GpuInfo("NVIDIA GeForce RTX 4080", "latest", null, null, null));

        assertTrue(DriverQuirkMatcher.match(cpu, gpus).isEmpty());
    }
}
