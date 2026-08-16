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
 * Windows OS build/version, as captured by {@code gpu.OsProbe} via WMI - the driver source
 * (Windows Update vs. OEM-provided) differs across the two confirmed reference machines, so the
 * exact OS build is real diagnostic signal, not incidental metadata.
 */
public record OsInfo(String caption, String version, String buildNumber, String architecture) {

    public static OsInfo unknown() {
        return new OsInfo(null, null, null, null);
    }
}
