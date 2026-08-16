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
 * One core shader discovered under {@code assets/minecraft/shaders/core/} in the real client jar.
 *
 * <p>{@code flagged} marks the shaders confirmed (by reading the real 26.2 jar, not the stale
 * {@code rendertype_solid}/{@code rendertype_translucent}/{@code rendertype_cutout} names in the
 * v1.5 spec docs, which do not exist as separate files in this MC version) to actually back the bug
 * report's symptoms: {@code terrain} (solid/cutout/translucent terrain - lava, water, and ordinary
 * blocks all render through this one shader, differentiated at compile time by the
 * {@code ALPHA_CUTOUT} define) and {@code rendertype_end_portal} (portals). Every other discovered
 * shader is still compiled and reported - the root cause is a hypothesis, not a confirmed
 * diagnosis, so the tool must not presuppose which shaders are actually broken.
 */
public record ShaderInventoryEntry(String name, boolean flagged, boolean hasVertex, boolean hasFragment) {
}
