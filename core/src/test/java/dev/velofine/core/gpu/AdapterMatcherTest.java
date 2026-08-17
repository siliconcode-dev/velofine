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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AdapterMatcherTest {

    @Test
    void identicalStringsMatch() {
        assertTrue(AdapterMatcher.matches("Intel(R) HD Graphics 4000", "Intel(R) HD Graphics 4000"));
    }

    @Test
    void trademarkMarkerDifferencesStillMatch() {
        // Real WMI/GL_RENDERER strings for the same hardware are known to differ on exactly this.
        assertTrue(AdapterMatcher.matches("Intel HD Graphics 4000", "Intel(R) HD Graphics 4000"));
    }

    @Test
    void differentAdaptersDoNotMatch() {
        assertFalse(AdapterMatcher.matches("NVIDIA GeForce RTX 4070", "Intel(R) HD Graphics 4000"));
    }

    @Test
    void nullOrBlankNeverMatches() {
        assertFalse(AdapterMatcher.matches(null, "Intel(R) HD Graphics 4000"));
        assertFalse(AdapterMatcher.matches("Intel(R) HD Graphics 4000", null));
        assertFalse(AdapterMatcher.matches("", ""));
    }
}
