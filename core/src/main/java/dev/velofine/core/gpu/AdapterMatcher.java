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

import java.util.Locale;

/**
 * Correlates a live {@code GL_RENDERER} string (what the GL context actually bound to,
 * post-context-creation) against a WMI-reported adapter name (pre-context detection) - on a
 * hybrid/switchable-graphics laptop, the two can disagree (WMI lists an Intel iGPU while the GL
 * context actually bound to a discrete NVIDIA/AMD part, or vice versa), which is exactly the risk
 * {@code Masterdoc_v1.5.md} S4 flags for the post-context confirmation gate.
 *
 * <p>Small, intentional duplication of {@code diagnostics-tool.gpu.AdapterMatcher}'s normalize-
 * and-compare algorithm, simplified to a single-adapter check since the live engine only ever
 * needs to confirm the one adapter {@code GpuDetector} already resolved, not search a list - same
 * "standalone tool doesn't depend on core" precedent {@code GpuProbe}'s own javadoc already sets,
 * applied in the other direction (core doesn't depend on the standalone tool either).
 */
public final class AdapterMatcher {

    private AdapterMatcher() {
    }

    /**
     * True if {@code glRenderer} and {@code adapterName} share a recognizable token once
     * trademark markers and punctuation are stripped. False (never a confident match) if either
     * is null/blank.
     */
    public static boolean matches(String glRenderer, String adapterName) {
        if (glRenderer == null || glRenderer.isBlank() || adapterName == null || adapterName.isBlank()) {
            return false;
        }
        String normalizedRenderer = normalize(glRenderer);
        String normalizedAdapter = normalize(adapterName);
        return normalizedRenderer.contains(normalizedAdapter) || normalizedAdapter.contains(normalizedRenderer);
    }

    private static String normalize(String value) {
        // Strips trademark markers as whole tokens FIRST - a naive "strip all punctuation" pass
        // would turn "Intel(R)" into "intelr" (the letter R survives, only the parens are
        // punctuation), which would then fail to match a renderer string that omits the mark
        // entirely. Real WMI/GL_RENDERER strings for the same hardware are known to differ on
        // exactly this (one may include "(R)"/"(TM)", the other may not).
        String withoutTrademarks = value.toLowerCase(Locale.ROOT).replaceAll("\\((r|tm|c)\\)", "");
        return withoutTrademarks.replaceAll("[^a-z0-9]", "");
    }
}
