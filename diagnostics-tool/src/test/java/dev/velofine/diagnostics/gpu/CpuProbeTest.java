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

import dev.velofine.diagnostics.model.CpuInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class CpuProbeTest {

    @Test
    void parsesSingleSocketJsonObject() {
        String json = "{\"Name\":\"Intel(R) Core(TM) i3-3110M CPU @ 2.40GHz  \",\"NumberOfCores\":2,\"NumberOfLogicalProcessors\":4}";
        CpuInfo info = CpuProbe.parse(json);

        assertEquals("Intel(R) Core(TM) i3-3110M CPU @ 2.40GHz", info.name());
        assertEquals(2, info.physicalCores());
        assertEquals(4, info.logicalProcessors());
    }

    @Test
    void parsesMultiSocketJsonArrayUsingFirstEntry() {
        String json = "[{\"Name\":\"CPU 0\",\"NumberOfCores\":4,\"NumberOfLogicalProcessors\":8},"
                + "{\"Name\":\"CPU 1\",\"NumberOfCores\":4,\"NumberOfLogicalProcessors\":8}]";
        CpuInfo info = CpuProbe.parse(json);

        assertEquals("CPU 0", info.name());
    }

    @Test
    void blankJsonReturnsUnknown() {
        CpuInfo info = CpuProbe.parse("");
        assertNull(info.name());
        assertNull(info.physicalCores());
    }
}
