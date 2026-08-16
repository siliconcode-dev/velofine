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

import java.util.Comparator;

/**
 * Compares plain dotted-numeric version strings (Velofine's own controlled scheme, e.g.
 * {@code "0.2.0"}) - deliberately not a full semver library, since Velofine never publishes
 * pre-release/build-metadata suffixes on {@link dev.velofine.core.BuildInfo#velofineVersion()}
 * itself (that information lives in the separate {@code targetMcVersion} manifest field instead).
 * A missing/non-numeric component compares as {@code 0}, so an odd string never throws - it just
 * sorts low.
 */
final class VersionComparator implements Comparator<String> {

    @Override
    public int compare(String a, String b) {
        int[] partsA = parts(a);
        int[] partsB = parts(b);
        int length = Math.max(partsA.length, partsB.length);
        for (int i = 0; i < length; i++) {
            int valueA = i < partsA.length ? partsA[i] : 0;
            int valueB = i < partsB.length ? partsB[i] : 0;
            int cmp = Integer.compare(valueA, valueB);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    boolean isNewer(String candidate, String current) {
        return compare(candidate, current) > 0;
    }

    private static int[] parts(String version) {
        String[] split = version.split("\\.");
        int[] result = new int[split.length];
        for (int i = 0; i < split.length; i++) {
            try {
                result[i] = Integer.parseInt(split[i].replaceAll("[^0-9].*$", ""));
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }
}
