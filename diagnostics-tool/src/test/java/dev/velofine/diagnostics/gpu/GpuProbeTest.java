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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class GpuProbeTest {

    @Test
    void parsesRealPnpDeviceIdFormat() {
        String[] result = GpuProbe.parsePnpId("PCI\\VEN_8086&DEV_0166&SUBSYS_00000000&REV_09");
        assertArrayEquals(new String[] {"0x8086", "0x0166"}, result);
    }

    @Test
    void lowercaseHexIsUppercasedForConsistency() {
        String[] result = GpuProbe.parsePnpId("PCI\\VEN_10de&DEV_1c8d&SUBSYS_00000000");
        assertArrayEquals(new String[] {"0x10DE", "0x1C8D"}, result);
    }

    @Test
    void nullPnpDeviceIdReturnsNulls() {
        assertArrayEquals(new String[] {null, null}, GpuProbe.parsePnpId(null));
    }

    @Test
    void unparsablePnpDeviceIdReturnsNullsRatherThanThrowing() {
        String[] result = GpuProbe.parsePnpId("SWD\\SOMETHING\\NOT_A_PCI_ID");
        assertNull(result[0]);
        assertNull(result[1]);
    }
}
