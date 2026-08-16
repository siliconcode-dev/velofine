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

import dev.velofine.diagnostics.model.AttributeInfo;
import dev.velofine.diagnostics.model.ProgramIntrospection;
import dev.velofine.diagnostics.model.UniformBlockInfo;
import dev.velofine.diagnostics.model.UniformInfo;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryStack;

/**
 * Reads back everything a real linked program actually resolved via {@code glGetActiveAttrib}/
 * {@code glGetActiveUniform}/active-uniform-block enumeration - catches "compiles and links fine,
 * but an attribute got silently dropped," and is the mechanism that lets the draw test (see
 * {@code DrawCallTester}) work generically across all 34 shaders' genuinely differing vertex
 * formats rather than needing a hardcoded per-shader catalog. Not unit tested: requires a real
 * linked program on a real GPU/driver context.
 */
public final class ProgramIntrospector {

    private ProgramIntrospector() {
    }

    public static ProgramIntrospection introspect(int program) {
        return new ProgramIntrospection(readAttributes(program), readUniforms(program), readUniformBlocks(program));
    }

    private static List<AttributeInfo> readAttributes(int program) {
        List<AttributeInfo> result = new ArrayList<>();
        int count = GL20.glGetProgrami(program, GL20.GL_ACTIVE_ATTRIBUTES);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            for (int i = 0; i < count; i++) {
                String name = GL20.glGetActiveAttrib(program, i, size, type);
                int location = GL20.glGetAttribLocation(program, name);
                result.add(new AttributeInfo(name, location, type.get(0), GlTypeNames.nameOf(type.get(0))));
            }
        }
        return result;
    }

    private static List<UniformInfo> readUniforms(int program) {
        List<UniformInfo> result = new ArrayList<>();
        int count = GL20.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORMS);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer size = stack.mallocInt(1);
            IntBuffer type = stack.mallocInt(1);
            for (int i = 0; i < count; i++) {
                String name = GL20.glGetActiveUniform(program, i, size, type);
                // Uniforms that belong to a named block (Fog/Globals/ChunkSection) surface separately
                // via readUniformBlocks() - glGetUniformLocation returns -1 for those, which is
                // expected and not an error.
                int location = GL20.glGetUniformLocation(program, name);
                result.add(new UniformInfo(name, location, type.get(0), GlTypeNames.nameOf(type.get(0))));
            }
        }
        return result;
    }

    private static List<UniformBlockInfo> readUniformBlocks(int program) {
        List<UniformBlockInfo> result = new ArrayList<>();
        int count = GL20.glGetProgrami(program, GL31.GL_ACTIVE_UNIFORM_BLOCKS);

        for (int i = 0; i < count; i++) {
            String name = GL31.glGetActiveUniformBlockName(program, i);
            int dataSize = GL31.glGetActiveUniformBlocki(program, i, GL31.GL_UNIFORM_BLOCK_DATA_SIZE);
            result.add(new UniformBlockInfo(name, i, dataSize));
        }
        return result;
    }

    /** Maps common GL type enums to readable names - real types seen across the 34 shaders. */
    static final class GlTypeNames {
        private GlTypeNames() {
        }

        static String nameOf(int glType) {
            if (glType == GL20.GL_FLOAT) {
                return "float";
            } else if (glType == GL20.GL_FLOAT_VEC2) {
                return "vec2";
            } else if (glType == GL20.GL_FLOAT_VEC3) {
                return "vec3";
            } else if (glType == GL20.GL_FLOAT_VEC4) {
                return "vec4";
            } else if (glType == GL20.GL_INT) {
                return "int";
            } else if (glType == GL20.GL_INT_VEC2) {
                return "ivec2";
            } else if (glType == GL20.GL_INT_VEC3) {
                return "ivec3";
            } else if (glType == GL20.GL_INT_VEC4) {
                return "ivec4";
            } else if (glType == GL20.GL_BOOL) {
                return "bool";
            } else if (glType == GL20.GL_FLOAT_MAT2) {
                return "mat2";
            } else if (glType == GL21.GL_FLOAT_MAT2x3 || glType == GL21.GL_FLOAT_MAT3x2) {
                return "mat2x3/mat3x2";
            } else if (glType == GL20.GL_FLOAT_MAT3) {
                return "mat3";
            } else if (glType == GL20.GL_FLOAT_MAT4) {
                return "mat4";
            } else if (glType == GL20.GL_SAMPLER_2D) {
                return "sampler2D";
            } else if (glType == GL20.GL_SAMPLER_3D) {
                return "sampler3D";
            } else if (glType == GL20.GL_SAMPLER_CUBE) {
                return "samplerCube";
            }
            return "0x" + Integer.toHexString(glType);
        }
    }
}
