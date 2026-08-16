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

import dev.velofine.diagnostics.model.ShaderPrecisionEntry;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.ARBES2Compatibility;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL41;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

/**
 * Queries {@code glGetShaderPrecisionFormat} for vertex/fragment float precision - old Intel GPUs
 * sometimes reduce fragment-shader float precision, surfaced directly rather than left as a guess.
 *
 * <p>Guards on capability first (same pattern {@code GlCapabilityProbe} already uses elsewhere in
 * this module): the function only exists via core GL 4.1 or the {@code GL_ARB_ES2_compatibility}
 * extension, and calling an unresolved LWJGL function pointer throws - old hardware realistically
 * lacks both, in which case {@link ShaderPrecisionEntry#unsupported()} is set rather than crashing.
 * Not unit tested: requires a real GL context.
 */
public final class ShaderPrecisionProbe {

    private static final int[] STAGES = {GL20.GL_VERTEX_SHADER, GL20.GL_FRAGMENT_SHADER};

    private ShaderPrecisionProbe() {
    }

    public static List<ShaderPrecisionEntry> probe() {
        List<ShaderPrecisionEntry> result = new ArrayList<>();
        GLCapabilities caps = GL.getCapabilities();
        boolean supported = caps.OpenGL41 || caps.GL_ARB_ES2_compatibility;

        for (int stage : STAGES) {
            String stageName = stage == GL20.GL_VERTEX_SHADER ? "vertex" : "fragment";
            if (!supported) {
                result.add(new ShaderPrecisionEntry(stageName, "float", true, 0, 0, 0));
                continue;
            }
            result.add(queryFloatPrecision(stageName, stage, "low", precisionConstant(caps, "low"), caps.OpenGL41));
            result.add(queryFloatPrecision(stageName, stage, "medium", precisionConstant(caps, "medium"), caps.OpenGL41));
            result.add(queryFloatPrecision(stageName, stage, "high", precisionConstant(caps, "high"), caps.OpenGL41));
        }
        return result;
    }

    private static int precisionConstant(GLCapabilities caps, String level) {
        if (caps.OpenGL41) {
            return switch (level) {
                case "low" -> GL41.GL_LOW_FLOAT;
                case "medium" -> GL41.GL_MEDIUM_FLOAT;
                default -> GL41.GL_HIGH_FLOAT;
            };
        }
        return switch (level) {
            case "low" -> ARBES2Compatibility.GL_LOW_FLOAT;
            case "medium" -> ARBES2Compatibility.GL_MEDIUM_FLOAT;
            default -> ARBES2Compatibility.GL_HIGH_FLOAT;
        };
    }

    private static ShaderPrecisionEntry queryFloatPrecision(
            String stageName, int glStage, String level, int precisionType, boolean useCore41) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer range = stack.mallocInt(2);
            IntBuffer precision = stack.mallocInt(1);
            if (useCore41) {
                GL41.glGetShaderPrecisionFormat(glStage, precisionType, range, precision);
            } else {
                ARBES2Compatibility.glGetShaderPrecisionFormat(glStage, precisionType, range, precision);
            }
            return new ShaderPrecisionEntry(stageName, "float_" + level, false, range.get(0), range.get(1), precision.get(0));
        }
    }
}
