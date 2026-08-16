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

package dev.velofine.core.updater;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VersionComparatorTest {

    private final VersionComparator comparator = new VersionComparator();

    @Test
    void higherPatchIsNewer() {
        assertTrue(comparator.isNewer("1.0.1", "1.0.0"));
        assertFalse(comparator.isNewer("1.0.0", "1.0.1"));
    }

    @Test
    void higherMinorOutweighsLowerPatch() {
        assertTrue(comparator.isNewer("1.1.0", "1.0.9"));
    }

    @Test
    void higherMajorOutweighsEverythingElse() {
        assertTrue(comparator.isNewer("2.0.0", "1.99.99"));
    }

    @Test
    void identicalVersionsAreNotNewer() {
        assertFalse(comparator.isNewer("1.0.0", "1.0.0"));
    }

    @Test
    void aMissingComponentComparesAsZero() {
        assertTrue(comparator.isNewer("1.1", "1.0.5"));
        assertFalse(comparator.isNewer("1.0", "1.0.1"));
    }

    @Test
    void aPreReleaseSuffixDoesNotBreakParsing() {
        // Velofine's own version string (project.version) can carry a suffix like "1.0.0-Beta" -
        // the numeric prefix of each dot-separated component must still parse.
        assertTrue(comparator.isNewer("1.0.1-Beta", "1.0.0-Beta"));
        assertFalse(comparator.isNewer("1.0.0-Beta", "1.0.0-Beta"));
    }
}
