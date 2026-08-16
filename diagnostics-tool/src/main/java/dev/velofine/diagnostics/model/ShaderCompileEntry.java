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
 * One compile attempt: a single shader name + stage + preprocessor-define variant.
 *
 * <p>{@code defineVariant} is {@code "default"} for a plain compile, or a define name (e.g.
 * {@code "ALPHA_CUTOUT"}) for a variant confirmed present in the real shader source via an
 * {@code #ifdef} - see {@code shader.DefineVariants}. This is how the tool distinguishes the
 * solid/translucent vs. cutout terrain paths without needing a full catalog of every
 * {@code RenderPipeline}'s {@code getShaderDefines()} output.
 */
public record ShaderCompileEntry(
        String shaderName,
        String stage,
        String defineVariant,
        String sourceOrigin,
        boolean missingFromJar,
        List<String> importChain,
        boolean importCycleDetected,
        ShaderCompileResult compile,
        String resolvedSourcePath,
        String sourceSha256) {
}
