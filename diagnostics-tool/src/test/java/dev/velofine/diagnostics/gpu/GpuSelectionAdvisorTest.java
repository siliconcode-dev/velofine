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

import static org.junit.jupiter.api.Assertions.assertTrue;

final class GpuSelectionAdvisorTest {

    private GpuInfo adapter(String name) {
        return new GpuInfo(name, null, null, null, null);
    }

    @Test
    void noAdviceWhenMatchWasConfident() {
        List<GpuInfo> adapters = List.of(adapter("Intel(R) UHD Graphics 620"), adapter("Radeon (TM) 520"));
        Optional<String> advice = GpuSelectionAdvisor.adviseIfNeeded(adapters, 0, "Intel(R) UHD Graphics 620");
        assertTrue(advice.isEmpty());
    }

    @Test
    void noAdviceWhenOnlyOneAdapterExists() {
        List<GpuInfo> adapters = List.of(adapter("Intel(R) HD Graphics 4000"));
        Optional<String> advice = GpuSelectionAdvisor.adviseIfNeeded(adapters, null, "some unrelated renderer");
        assertTrue(advice.isEmpty());
    }

    @Test
    void advisesWhenMultipleAdaptersAndNoConfidentMatch() {
        List<GpuInfo> adapters = List.of(adapter("Intel(R) HD Graphics 4000"), adapter("NVIDIA GeForce GT 650M"));
        Optional<String> advice = GpuSelectionAdvisor.adviseIfNeeded(adapters, null, "AMD Radeon RX 6800");

        assertTrue(advice.isPresent());
        assertTrue(advice.get().contains("High performance"));
    }
}
