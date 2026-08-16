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

/**
 * Fixed asset filenames every release published by {@code .github/workflows/release.yml} carries.
 * The installer exe's filename is versioned ({@link #installerName(String)}); the other three are
 * identical across every release so the client never needs to guess a pattern for them.
 */
final class ReleaseAssets {

    static final String MANIFEST = "manifest.json";
    static final String CHECKSUMS = "SHA256SUMS.txt";
    static final String CHECKSUMS_SIGNATURE = "SHA256SUMS.txt.sig";

    private ReleaseAssets() {
    }

    static String installerName(String velofineVersion) {
        return "Velofine-Setup-" + velofineVersion + ".exe";
    }
}
