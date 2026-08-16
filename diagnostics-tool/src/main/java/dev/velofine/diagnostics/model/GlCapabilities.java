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
 * GL_MAX_* limits and derived support flags relevant to the modern uniform-buffer-object /
 * vertex-array-driven rendering path the v1.5 root-cause hypothesis implicates (see
 * Masterdoc_v1.5.md S2). Captured broadly rather than narrowed to a single suspected value, since
 * the root cause is unconfirmed.
 */
public record GlCapabilities(
        int maxUniformBufferBindings,
        int maxUniformBlockSize,
        int maxVertexAttribs,
        int maxCombinedTextureImageUnits,
        int maxArrayTextureLayers,
        int maxDrawBuffers,
        boolean supportsUniformBufferObject,
        boolean supportsVertexArrayObject,
        boolean supportsMultiDrawIndirect) {
}
