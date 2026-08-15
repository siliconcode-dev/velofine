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
 * Whether the game directory's drive is a spinning HDD, as reported by Windows Storage Management
 * ({@code Get-PhysicalDisk}'s {@code MediaType}).
 *
 * @param rotational {@code true} if the underlying physical disk reports {@code MediaType = HDD}.
 */
public record DiskInfo(boolean rotational) {

    public static DiskInfo unknown() {
        return new DiskInfo(false);
    }
}
