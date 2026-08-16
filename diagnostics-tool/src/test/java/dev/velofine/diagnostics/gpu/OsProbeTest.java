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

import dev.velofine.diagnostics.model.OsInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class OsProbeTest {

    @Test
    void parsesRealWmiShape() {
        String json = "{\"Caption\":\"Microsoft Windows 11 Home Single Language\",\"Version\":\"10.0.26200\","
                + "\"BuildNumber\":\"26200\",\"OSArchitecture\":\"64-bit\"}";
        OsInfo info = OsProbe.parse(json);

        assertEquals("Microsoft Windows 11 Home Single Language", info.caption());
        assertEquals("10.0.26200", info.version());
        assertEquals("26200", info.buildNumber());
        assertEquals("64-bit", info.architecture());
    }

    @Test
    void blankJsonReturnsUnknown() {
        OsInfo info = OsProbe.parse(null);
        assertNull(info.caption());
    }
}
