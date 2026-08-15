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

/**
 * Total physical RAM installed, as reported by Windows.
 *
 * @param totalPhysicalBytes total physical memory in bytes, or -1 if detection failed.
 */
public record MemoryInfo(long totalPhysicalBytes) {

    /**
     * Reference hardware (Build_plan/Masterdoc) is documented as 4GB DDR3 on both machines; actual
     * reported {@code TotalPhysicalMemory} for a "4GB" stick is always somewhat under 4 * 1024^3
     * (reserved/shared memory), so this ceiling leaves headroom to still classify a real 4GB
     * machine as low-memory while not catching common 8GB configurations.
     */
    private static final long LOW_MEMORY_CEILING_BYTES = 6L * 1024 * 1024 * 1024;

    public static MemoryInfo unknown() {
        return new MemoryInfo(-1);
    }

    public boolean isLowMemory() {
        return totalPhysicalBytes > 0 && totalPhysicalBytes <= LOW_MEMORY_CEILING_BYTES;
    }
}
