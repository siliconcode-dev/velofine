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
 * One real active vertex attribute, as reported by {@code glGetActiveAttrib} - {@code glType} is
 * the raw GL type enum (e.g. {@code GL_FLOAT_VEC3}, {@code GL_INT_VEC2}); real confirmed types
 * across the 34 shaders include both, per {@code entity.vsh}'s integer {@code ivec2 UV1}/{@code UV2}.
 */
public record AttributeInfo(String name, int location, int glType, String glTypeName) {
}
