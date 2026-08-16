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

import dev.velofine.diagnostics.model.GpuInfo;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Correlates the live {@code GL_RENDERER} string (what the GL context actually bound to) against
 * WMI's adapter list - on a hybrid/switchable-graphics laptop (the exact risk
 * Masterdoc_v1.5.md flags for the reference machines), WMI can list an Intel iGPU while the GL
 * context actually bound to a discrete NVIDIA/AMD part, or vice versa. Pure string-matching, no
 * WMI/GL dependency of its own - fully unit-testable.
 */
public final class AdapterMatcher {

    private AdapterMatcher() {
    }

    /**
     * Returns the index into {@code adapters} whose {@code adapterName} shares a recognizable
     * token with {@code glRenderer}, or empty if no adapter is a confident match (e.g. the GL
     * context bound to a GPU WMI didn't report, or the strings simply don't overlap).
     */
    public static Optional<Integer> matchRendererToAdapter(String glRenderer, List<GpuInfo> adapters) {
        if (glRenderer == null || glRenderer.isBlank() || adapters == null) {
            return Optional.empty();
        }
        String normalizedRenderer = normalize(glRenderer);

        for (int i = 0; i < adapters.size(); i++) {
            String adapterName = adapters.get(i).adapterName();
            if (adapterName == null || adapterName.isBlank()) {
                continue;
            }
            String normalizedAdapter = normalize(adapterName);
            if (normalizedRenderer.contains(normalizedAdapter) || normalizedAdapter.contains(normalizedRenderer)) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
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
