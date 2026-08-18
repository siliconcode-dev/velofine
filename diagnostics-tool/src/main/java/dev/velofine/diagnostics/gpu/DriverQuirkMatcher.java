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
            } else if (isMachineBGpu(gpu)) {
                notes.add("GPU matches reference machine B (i5-3470S / HD Graphics 2500). CPU model "
                        + (matchesCpu(cpu, "i5-3470S") ? "also matches" : "was not confirmed as i5-3470S")
                        + " - driver provenance (Windows-provided vs. Intel-provided) can't be determined "
                        + "from the driver version string alone.");
            } else if (GEN7_PATTERN.matcher(gpu.adapterName()).find()) {
                notes.add("GPU (\"" + gpu.adapterName() + "\") matches the broader Ivy Bridge Gen7 Intel iGPU "
                        + "family but not an exact-verified reference machine - eligible only for the "
                        + "conservative compatibility-renderer fallback (Phase 3), not the targeted fix.");
            }
        }
        return notes;
    }

    private static boolean isExactMachineA(CpuInfo cpu, GpuInfo gpu) {
        return matchesCpu(cpu, "i3-3110M")
                && gpu.adapterName().toLowerCase().contains("hd graphics 4000")
                && driverMatches(gpu.driverVersion(), "15.33.53.5161");
    }

    private static boolean isMachineBGpu(GpuInfo gpu) {
        return gpu.adapterName().toLowerCase().contains("hd graphics 2500");
    }

    private static boolean matchesCpu(CpuInfo cpu, String model) {
        return cpu != null && cpu.name() != null && cpu.name().contains(model);
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
