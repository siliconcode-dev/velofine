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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Matches captured {@link CpuInfo}/{@link GpuInfo} against the two confirmed v1.5 reference
 * machines (Masterdoc_v1.5.md S3), plus the broader Ivy Bridge Gen7 Intel iGPU family. Purely
 * informational - kept in the report's own {@code knownQuirkNotes} field, separate from
 * {@code toolWarnings}, so it's never mistaken for pass/fail signal.
 */
public final class DriverQuirkMatcher {

    private static final Pattern GEN7_PATTERN = Pattern.compile("Intel.*HD Graphics (2500|4000)", Pattern.CASE_INSENSITIVE);

    // v1.8-Beta: WMI does not always report the iGPU's model number - on the real reference machine B
    // it returns the bare "Intel(R) HD Graphics". Mirrors core.gpu.GpuDetector.IVY_BRIDGE_CPU_PATTERN;
    // Ivy Bridge only ever shipped HD 2500 and HD 4000, so an Ivy Bridge CPU pins a generic Intel
    // adapter to Gen7. Keeping this in lockstep with core matters - this class's whole purpose is that
    // a tester reading "matches reference machine B" sees the same verdict the live agent reached.
    private static final Pattern IVY_BRIDGE_CPU_PATTERN = Pattern.compile("Core.*i[357]-3\\d{3}", Pattern.CASE_INSENSITIVE);

    private static final Pattern INTEL_HD_GRAPHICS_PATTERN = Pattern.compile("Intel.*HD Graphics", Pattern.CASE_INSENSITIVE);

    private DriverQuirkMatcher() {
    }

    public static List<String> match(CpuInfo cpu, List<GpuInfo> adapters) {
        List<String> notes = new ArrayList<>();
        if (adapters == null) {
            return notes;
        }

        for (GpuInfo gpu : adapters) {
            if (gpu.adapterName() == null) {
                continue;
            }
            if (isExactMachineA(cpu, gpu)) {
                notes.add("Matches reference machine A (i3-3110M / HD Graphics 4000, driver 15.33.53.5161 "
                        + "/ 10.18.10.5161) exactly - eligible for the full targeted shader-patch fix once "
                        + "available (Phase 4).");
            } else if (isMachineB(cpu, gpu)) {
                notes.add("Matches reference machine B (i5-3470S / HD Graphics 2500). "
                        + machineBIdentification(cpu, gpu)
                        + " Driver provenance (Windows-provided vs. Intel-provided) can't be determined "
                        + "from the driver version string alone.");
            } else if (isGen7Family(cpu, gpu)) {
                notes.add("GPU (\"" + gpu.adapterName() + "\") matches the broader Ivy Bridge Gen7 Intel iGPU "
                        + "family but not an exact-verified reference machine - eligible only for the "
                        + "conservative compatibility-renderer fallback (Phase 3), not the targeted fix.");
            }
        }
        return notes;
    }

    private static boolean isExactMachineA(CpuInfo cpu, GpuInfo gpu) {
        return matchesCpu(cpu, "i3-3110M")
                && INTEL_HD_GRAPHICS_PATTERN.matcher(gpu.adapterName()).find()
                && driverMatches(gpu.driverVersion(), "15.33.53.5161");
    }

    /**
     * Spells out <em>which</em> signal identified the machine, since the two carry different
     * confidence: the GPU model alone leaves the CPU unconfirmed (a different Ivy Bridge desktop could
     * share that iGPU), whereas the CPU model is unique to this machine.
     */
    private static String machineBIdentification(CpuInfo cpu, GpuInfo gpu) {
        boolean gpuModelMatches = gpu.adapterName().toLowerCase().contains("hd graphics 2500");
        boolean cpuMatches = matchesCpu(cpu, "i5-3470S");
        if (gpuModelMatches && cpuMatches) {
            return "GPU and CPU model both match.";
        }
        if (gpuModelMatches) {
            return "GPU model matches; CPU model was not confirmed as i5-3470S.";
        }
        return "Identified by CPU model - WMI reported no GPU model number for this adapter "
                + "(\"" + gpu.adapterName() + "\"), which is itself expected on this machine.";
    }

    /**
     * Model name when WMI supplies it, otherwise the CPU - which uniquely identifies this machine.
     * The Intel-adapter conjunct keeps a discrete card in an i5-3470S desktop from matching.
     */
    private static boolean isMachineB(CpuInfo cpu, GpuInfo gpu) {
        return gpu.adapterName().toLowerCase().contains("hd graphics 2500")
                || (matchesCpu(cpu, "i5-3470S") && INTEL_HD_GRAPHICS_PATTERN.matcher(gpu.adapterName()).find());
    }

    private static boolean isGen7Family(CpuInfo cpu, GpuInfo gpu) {
        if (GEN7_PATTERN.matcher(gpu.adapterName()).find()) {
            return true;
        }
        String cpuName = cpu != null ? cpu.name() : null;
        return cpuName != null
                && IVY_BRIDGE_CPU_PATTERN.matcher(cpuName).find()
                && INTEL_HD_GRAPHICS_PATTERN.matcher(gpu.adapterName()).find();
    }

    /** Case-insensitive, matching {@code core.gpu.LegacyGpuRegistry}'s own containsIgnoreCase. */
    private static boolean matchesCpu(CpuInfo cpu, String model) {
        return cpu != null && cpu.name() != null && cpu.name().toLowerCase().contains(model.toLowerCase());
    }

    /**
     * Mirrors {@code core.gpu.LegacyGpuRegistry}'s driver-matching rule exactly (per this class's
     * own precedent of deliberately mirroring core's criteria): Intel reports the same physical
     * driver under two version schemes (package version, e.g. {@code 15.33.53.5161}, vs. the
     * WMI/PnP-reported version, e.g. {@code 10.18.10.5161}) that only share their trailing build
     * segment - confirmed against a real tester report from reference machine A and independently
     * on Intel's own community forum. Match on that shared segment rather than full-string equality.
     */
    private static boolean driverMatches(String actual, String expected) {
        if (actual == null || expected == null) {
            return false;
        }
        return expected.equals(actual) || trailingSegment(actual).equals(trailingSegment(expected));
    }

    private static String trailingSegment(String driverVersion) {
        int lastDot = driverVersion.lastIndexOf('.');
        return lastDot >= 0 ? driverVersion.substring(lastDot + 1) : driverVersion;
    }
}
