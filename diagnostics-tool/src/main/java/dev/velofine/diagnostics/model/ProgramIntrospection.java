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
 * Everything {@code gl.ProgramIntrospector} found on a real linked program - catches "compiles and
 * links fine, but an attribute got silently dropped," which link-status-only testing would miss.
 * Also the mechanism that makes the draw test (item 6) generic across all 34 shaders' genuinely
 * differing vertex formats, rather than a hardcoded per-shader catalog.
 */
public record ProgramIntrospection(
        List<AttributeInfo> attributes, List<UniformInfo> uniforms, List<UniformBlockInfo> uniformBlocks) {
}
