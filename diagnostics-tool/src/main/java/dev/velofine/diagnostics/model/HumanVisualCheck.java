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
 * A tester's own in-game observation, optionally attached to a report - not derived from anything
 * this tool measures itself. Build_plan_v1.5's own design already treats a human's eyes as the real
 * final visual-correctness check (the synthesized draw test is explicitly a robustness check, not a
 * substitute for actually looking at lava/water/portals/blocks in-game); this just gives that
 * observation a structured home in the same report instead of living only in a chat message to the
 * founder. Every field is nullable - a tester who only ran the tool without also launching the real
 * Velofine build in-game has nothing to report here, and that's a legitimate, common case, not an
 * error.
 */
public record HumanVisualCheck(
        Boolean lavaWaterPortalsVisible,
        Boolean previouslyBlackBlocksCorrect,
        String notes) {
}
