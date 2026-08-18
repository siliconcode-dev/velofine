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

package dev.velofine.diagnostics.gl;

import dev.velofine.diagnostics.model.UniformInfo;
import java.util.List;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;

/**
 * Sets plausible, non-zero values on every real active (non-block) uniform a program declares,
 * introspection-driven the same way {@link VertexFormatSynthesizer} handles attributes - required
 * because before this class existed, nothing populated plain (non-UBO) uniforms at all, so any
 * shader whose {@code ModelViewMat}/{@code ProjMat} are declared as plain uniforms rather than a
 * std140 block member (confirmed real for some of the 34 shaders - not every one uses the
 * {@code ChunkSection}/{@code Globals}/{@code Fog} blocks {@link UboRoundTripTester} knows about)
 * transformed every vertex with an all-zero matrix, which cannot project geometry anywhere
 * meaningful in clip space.
 *
 * <p>Matrix uniforms (mat2/mat3/mat4) get an identity matrix - the only value guaranteed not to
 * degenerate a transform regardless of what the matrix represents. Everything else gets a small,
 * uniform non-zero constant, since GL's own default (zero) is exactly the value that makes scale
 * factors, texture-coordinate offsets, and color multipliers vanish. Sampler uniforms are left
 * untouched here - {@code TextureBinder} (Fix 3b) owns assigning them a real texture unit, since
 * that decision depends on which real texture (if any) got bound to which unit. Not unit tested:
 * requires a real linked program on a real GL context.
 */
public final class UniformDefaults {

    private static final float CONSTANT = 1.0f;

    private UniformDefaults() {
    }

    /** Caller must have already called {@code glUseProgram(program)}. */
    public static void apply(int program, List<UniformInfo> uniforms) {
        for (UniformInfo uniform : uniforms) {
            if (uniform.location() < 0 || isSampler(uniform.glType())) {
                continue;
            }
            setDefault(uniform.location(), uniform.glType());
        }
    }

    private static void setDefault(int location, int glType) {
        if (glType == GL20.GL_FLOAT_MAT4) {
            GL20.glUniformMatrix4fv(location, false, identity(4));
        } else if (glType == GL20.GL_FLOAT_MAT3) {
            GL20.glUniformMatrix3fv(location, false, identity(3));
        } else if (glType == GL20.GL_FLOAT_MAT2) {
            GL20.glUniformMatrix2fv(location, false, identity(2));
        } else if (glType == GL21.GL_FLOAT_MAT2x3 || glType == GL21.GL_FLOAT_MAT3x2
                || glType == GL21.GL_FLOAT_MAT2x4 || glType == GL21.GL_FLOAT_MAT4x2
                || glType == GL21.GL_FLOAT_MAT3x4 || glType == GL21.GL_FLOAT_MAT4x3) {
            // Rare non-square matrix uniform - no single "identity" is well-defined, and none of
            // the 34 real shaders are known to declare one; a uniform non-zero fill still beats
            // leaving it at GL's all-zero default.
            GL20.glUniform4f(location, CONSTANT, CONSTANT, CONSTANT, CONSTANT);
        } else if (glType == GL20.GL_FLOAT) {
            GL20.glUniform1f(location, CONSTANT);
        } else if (glType == GL20.GL_FLOAT_VEC2) {
            GL20.glUniform2f(location, CONSTANT, CONSTANT);
        } else if (glType == GL20.GL_FLOAT_VEC3) {
            GL20.glUniform3f(location, CONSTANT, CONSTANT, CONSTANT);
        } else if (glType == GL20.GL_FLOAT_VEC4) {
            GL20.glUniform4f(location, CONSTANT, CONSTANT, CONSTANT, CONSTANT);
        } else if (glType == GL20.GL_INT || glType == GL20.GL_BOOL) {
            GL20.glUniform1i(location, 1);
        } else if (glType == GL20.GL_INT_VEC2) {
            GL20.glUniform2i(location, 1, 1);
        } else if (glType == GL20.GL_INT_VEC3) {
            GL20.glUniform3i(location, 1, 1, 1);
        } else if (glType == GL20.GL_INT_VEC4) {
            GL20.glUniform4i(location, 1, 1, 1, 1);
        }
        // Any other/unrecognized type is left at GL's default rather than guessed at further.
    }

    private static boolean isSampler(int glType) {
        return glType == GL20.GL_SAMPLER_2D || glType == GL20.GL_SAMPLER_CUBE || glType == GL20.GL_SAMPLER_3D;
    }

    private static float[] identity(int size) {
        float[] m = new float[size * size];
        for (int i = 0; i < size; i++) {
            m[i * size + i] = 1.0f;
        }
        return m;
    }
}
