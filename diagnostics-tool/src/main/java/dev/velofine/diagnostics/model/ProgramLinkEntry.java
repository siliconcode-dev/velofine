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
 * Result of linking a shader name's vertex+fragment pair together via {@code glLinkProgram}.
 *
 * <p>A shader can compile cleanly per-stage and still fail at link time (attribute/varying
 * mismatches) - compile-only testing would silently miss that failure mode entirely.
 *
 * <p>{@code introspection}/{@code drawTest} are nullable, and {@code uboTests} is empty, when
 * {@code success} is false - a failed link has no live program to probe. {@code uboTests} is a
 * list, not a single result, because a real shader can have more than one of the three confirmed
 * blocks active at once (e.g. {@code terrain} has {@code Fog}+{@code Globals}+{@code ChunkSection}
 * all active simultaneously). Nested here (not as parallel top-level report lists) so everything
 * about one linked program stays co-located in the JSON.
 */
public record ProgramLinkEntry(
        String shaderName,
        String defineVariant,
        boolean success,
        String infoLog,
        ProgramIntrospection introspection,
        DrawTestResult drawTest,
        List<UboRoundTripResult> uboTests) {
}
