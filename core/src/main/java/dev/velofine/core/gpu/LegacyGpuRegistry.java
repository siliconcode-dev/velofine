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

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Sodium-{@code Workarounds}-style signature registry for v1.5's two personally-verified
 * reference machines (see {@code Masterdoc_v1.5.md} S3), plus the broader Ivy Bridge Gen7 Intel
 * iGPU family {@code GpuDetector} already recognized in v1.
 *
 * <p>Deliberately mirrors {@code diagnostics-tool.gpu.DriverQuirkMatcher}'s exact-match rules
 * (built in v1.5 Phase 1) rather than inventing new criteria - keeping the standalone diagnostic
 * tool's advisory classification and the live engine's actual fix-eligibility classification in
 * agreement is the point; a tester reading "matches reference machine A" in a diagnostic report
 * should mean the same thing the live agent decided.
 *
 * <p>Pure and side-effect-free - no WMI/process calls of its own, callers supply the already-
 * detected {@link GpuInfo}/{@link CpuInfo}.
 */
public final class LegacyGpuRegistry {

    private record Signature(BiPredicate<GpuInfo, CpuInfo> matches) {
    }

    private static final String REFERENCE_A_DRIVER = "15.33.53.5161";

    private static final List<Signature> EXACT_SIGNATURES = List.of(
            // Reference machine A: i3-3110M / HD Graphics 4000 / Intel driver 15.33.53.5161.
            // Requires all three - HD Graphics 4000 alone is common across many Ivy Bridge CPUs,
            // and only this exact CPU+GPU+driver combination has actually been personally verified.
            new Signature((gpu, cpu) -> containsIgnoreCase(gpu.adapterName(), "HD Graphics 4000")
                    && driverMatches(gpu.driverVersion(), REFERENCE_A_DRIVER)
                    && containsIgnoreCase(cpu.name(), "i3-3110M")),
            // Reference machine B: i5-3470S / HD Graphics 2500, Windows-provided driver. No fixed
            // driver-version string exists to check (Windows Update ships whatever it currently
            // ships) - the GPU model itself is specific enough, matching
            // DriverQuirkMatcher.isMachineBGpu's already-established logic.
            new Signature((gpu, cpu) -> containsIgnoreCase(gpu.adapterName(), "HD Graphics 2500"))
    );

    private LegacyGpuRegistry() {
    }

    public static GpuConfidence classify(GpuInfo gpu, CpuInfo cpu) {
        if (gpu == null || gpu.adapterName() == null) {
            return GpuConfidence.NONE;
        }
        CpuInfo safeCpu = cpu != null ? cpu : CpuInfo.unknown();

        for (Signature signature : EXACT_SIGNATURES) {
            if (signature.matches().test(gpu, safeCpu)) {
                return GpuConfidence.EXACT_VERIFIED;
            }
        }

        if (gpu.fixProfile() != GpuInfo.FixProfile.NONE) {
            return GpuConfidence.FAMILY_MATCH;
        }

        return GpuConfidence.NONE;
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    /**
     * Intel ships every driver under two version strings for the same physical build - a
     * "package" version (e.g. {@code 15.33.53.5161}, what {@code Masterdoc_v1.5.md} S3 records)
     * and the WMI/PnP-reported "driver" version (e.g. {@code 10.18.10.5161}, what
     * {@code Win32_VideoController.DriverVersion} actually returns and what {@link GpuDetector}
     * captures) - confirmed against a real tester report from reference machine A itself, and
     * independently on Intel's own community forum. Only the trailing build segment is shared
     * between the two schemes, so an exact full-string match against the package-version literal
     * can never succeed against real WMI data; match on the trailing segment instead, falling back
     * to full-string equality in case a future caller ever does supply the package-version form
     * directly.
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
