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

import dev.velofine.core.gpu.GpuInfo;

/**
 * A rough 1-10 estimate for the Overview page's hardware panel (item 10), built entirely from
 * signals {@link HardwareProfiles} already detects and caches - no new detection work, no
 * benchmarking. Deliberately a coarse heuristic, not a measured number: v1.5's own hardware-fix
 * confidence tiering ({@code GpuConfidence}) is itself unverified on most real machines, so this
 * doesn't try to claim more precision than that.
 */
public final class PerformanceEstimate {

    private static final int MAX_SCORE = 10;

    private PerformanceEstimate() {
    }

    public static int score(HardwareProfile profile) {
        int score = MAX_SCORE;
        if (profile.gpu().fixProfile() == GpuInfo.FixProfile.INTEL_GEN7) {
            score -= 4;
        } else if (profile.gpu().fixProfile() == GpuInfo.FixProfile.GENERIC_OLD) {
            score -= 2;
        }
        if (profile.memory().isLowMemory()) {
            score -= 2;
        }
        if (profile.disk().rotational()) {
            score -= 2;
        }
        return Math.max(1, Math.min(MAX_SCORE, score));
    }

    /** Plain "N/10" number - no bar/glyph, just the rating. */
    public static String label(HardwareProfile profile) {
        return score(profile) + "/" + MAX_SCORE;
    }
}
