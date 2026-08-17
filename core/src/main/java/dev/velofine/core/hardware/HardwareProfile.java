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

import dev.velofine.core.gpu.CpuInfo;
import dev.velofine.core.gpu.GpuInfo;

/**
 * A single snapshot of everything LegacySupport's fix-selection logic ({@link FixProfileRules})
 * reads: detected CPU, GPU, physical RAM, and whether the game directory's drive is rotational.
 * Built once at agent-attach time.
 *
 * <p>{@code cpu} was added in v1.5 Phase 2 - {@code GpuInfo.confidence()} needs the CPU model to
 * distinguish reference machine A (i3-3110M + HD Graphics 4000 + a specific driver) from any other
 * Ivy Bridge laptop sharing the same GPU and driver version. No existing {@link FixProfileRules}
 * rule reads it directly (yet) - it flows through {@code GpuDetector} into {@code GpuInfo} instead.
 */
public record HardwareProfile(CpuInfo cpu, GpuInfo gpu, MemoryInfo memory, DiskInfo disk) {

    public static HardwareProfile unknown() {
        return new HardwareProfile(CpuInfo.unknown(), GpuInfo.unknown(), MemoryInfo.unknown(), DiskInfo.unknown());
    }
}
