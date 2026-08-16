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

package dev.velofine.diagnostics.model;

/**
 * The real CPU model, as captured by {@code gpu.CpuProbe} via WMI - correlates hardware identity
 * with the exact reference CPUs (i3-3110M / i5-3470S) alongside the GPU string.
 */
public record CpuInfo(String name, Integer physicalCores, Integer logicalProcessors) {

    public static CpuInfo unknown() {
        return new CpuInfo(null, null, null);
    }
}
