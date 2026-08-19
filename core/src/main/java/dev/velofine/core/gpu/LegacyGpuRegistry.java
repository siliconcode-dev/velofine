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
            // Driver + CPU are jointly decisive on their own, so the GPU clause accepts either the
            // full model name or the generic "Intel ... HD Graphics" form WMI sometimes reports
            // instead (see machine B's note below for the confirmed case of that happening).
            new Signature((gpu, cpu) -> isIntelHdGraphics(gpu.adapterName())
                    && driverMatches(gpu.driverVersion(), REFERENCE_A_DRIVER)
                    && containsIgnoreCase(cpu.name(), "i3-3110M")),
            // Reference machine B: i5-3470S / HD Graphics 2500, Windows-provided driver. No fixed
            // driver-version string exists to check (Windows Update ships whatever it currently
            // ships).
            //
            // v1.8-Beta: the original rule matched on the GPU model name alone, on the reasoning that
            // "the GPU model itself is specific enough". That turned out to be wrong on the actual
            // reference machine - a real v1.7-Beta tester log shows WMI reporting the bare string
            // "Intel(R) HD Graphics" with no model number, so this signature never matched, the
            // machine classified as FAMILY_MATCH, and both EXACT_VERIFIED-gated fixes silently stayed
            // off. (Same class of discovery as the driverMatches javadoc below: WMI reports something
            // different from what the hardware documentation says.) The CPU model is the decisive
            // signal - "i5-3470S" identifies this machine uniquely - so it is now accepted as an
            // alternative to the model name. The Intel-adapter conjunct is required so that a discrete
            // card added to an i5-3470S desktop can never be classified as the reference iGPU.
            new Signature((gpu, cpu) -> containsIgnoreCase(gpu.adapterName(), "HD Graphics 2500")
                    || (containsIgnoreCase(cpu.name(), "i5-3470S") && isIntelHdGraphics(gpu.adapterName())))
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
     * True for any Intel HD Graphics-branded adapter, model number present or not. Guards the
     * CPU-driven clauses above so a discrete GPU in a reference-CPU machine is never mistaken for
     * the reference iGPU.
     */
    private static boolean isIntelHdGraphics(String adapterName) {
        return containsIgnoreCase(adapterName, "Intel") && containsIgnoreCase(adapterName, "HD Graphics");
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
