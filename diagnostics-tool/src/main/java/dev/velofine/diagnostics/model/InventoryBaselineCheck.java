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

import java.util.List;

/**
 * Diffs a run's discovered shader inventory against the bundled real-34-names baseline for the
 * matching MC version - flags a corrupted/wrong-version jar before a run wastes time on bad input.
 * {@code baselineAvailable=false} when the tested MC version has no bundled baseline (not itself a
 * problem - just means this check is skipped for versions other than the one it was captured for).
 */
public record InventoryBaselineCheck(boolean baselineAvailable, List<String> unexpectedShaders, List<String> missingExpectedShaders) {

    public static InventoryBaselineCheck unavailable() {
        return new InventoryBaselineCheck(false, List.of(), List.of());
    }
}
