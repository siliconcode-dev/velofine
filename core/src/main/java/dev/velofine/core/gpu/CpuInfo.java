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

/**
 * Detected CPU model, as reported by Windows. Only the name is captured - unlike {@code
 * diagnostics-tool}'s report-oriented {@code CpuInfo}, the live engine has exactly one consumer
 * ({@link LegacyGpuRegistry}'s reference-machine-A exact-match rule, which needs to distinguish
 * an i3-3110M HD 4000 from any other Ivy Bridge laptop that happens to share the same GPU+driver),
 * so core count/thread count would be unused weight.
 */
public record CpuInfo(String name) {

    public static CpuInfo unknown() {
        return new CpuInfo(null);
    }
}
