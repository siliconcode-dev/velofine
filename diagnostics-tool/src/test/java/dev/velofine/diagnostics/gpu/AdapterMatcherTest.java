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
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdapterMatcherTest {

    private GpuInfo adapter(String name) {
        return new GpuInfo(name, null, null, null, null);
    }

    @Test
    void matchesRealWmiNameAgainstRealGlRendererDespiteRegistryMarkDifference() {
        // Real, confirmed divergence: WMI reports "Intel(R) HD Graphics 4000", GL_RENDERER reports
        // "Intel(R) HD Graphics 4000" too on most drivers, but some report without the (R) mark -
        // the normalization must tolerate punctuation differences either way.
        List<GpuInfo> adapters = List.of(adapter("Intel(R) HD Graphics 4000"));
        Optional<Integer> match = AdapterMatcher.matchRendererToAdapter("Intel HD Graphics 4000", adapters);

        assertTrue(match.isPresent());
        assertEquals(0, match.get());
    }

    @Test
    void picksCorrectAdapterAmongMultipleOnAHybridLaptop() {
        List<GpuInfo> adapters = List.of(adapter("Intel(R) HD Graphics 4000"), adapter("NVIDIA GeForce GT 650M"));
        Optional<Integer> match = AdapterMatcher.matchRendererToAdapter("NVIDIA GeForce GT 650M/PCIe/SSE2", adapters);

        assertTrue(match.isPresent());
        assertEquals(1, match.get());
    }

    @Test
    void noMatchWhenRendererSharesNothingWithAnyAdapter() {
        List<GpuInfo> adapters = List.of(adapter("Intel(R) HD Graphics 4000"));
        Optional<Integer> match = AdapterMatcher.matchRendererToAdapter("AMD Radeon RX 6800", adapters);

        assertTrue(match.isEmpty());
    }

    @Test
    void nullOrBlankInputsReturnEmptyRatherThanThrowing() {
        assertTrue(AdapterMatcher.matchRendererToAdapter(null, List.of(adapter("x"))).isEmpty());
        assertTrue(AdapterMatcher.matchRendererToAdapter("", List.of(adapter("x"))).isEmpty());
        assertTrue(AdapterMatcher.matchRendererToAdapter("x", null).isEmpty());
    }
}
