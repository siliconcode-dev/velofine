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
import java.nio.ByteBuffer;
import java.util.List;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

/**
 * Given a program's introspected attribute list, builds a zero-filled single-vertex VAO/VBO
 * generically - float attribs bound via {@code glVertexAttribPointer}, integer attribs via
 * {@code glVertexAttribIPointer}. This generic, introspection-driven approach is required (not
 * optional): real vertex formats genuinely differ per shader, confirmed by extracting real source
 * ({@code terrain.vsh} has 4 attribs, {@code entity.vsh} has 6 with 2 integer ones,
 * {@code rendertype_end_portal.vsh} has just 1) - no single hardcoded format covers all 34 shaders.
 * Not unit tested: requires a real GL context.
 */
public final class VertexFormatSynthesizer implements AutoCloseable {

    private final int vao;
    private final int vbo;

    private VertexFormatSynthesizer(int vao, int vbo) {
        this.vao = vao;
        this.vbo = vbo;
    }

    public int vao() {
        return vao;
    }

    public static VertexFormatSynthesizer build(List<AttributeInfo> attributes) {
        int stride = 0;
        int[] offsets = new int[attributes.size()];
        for (int i = 0; i < attributes.size(); i++) {
            offsets[i] = stride;
            stride += componentInfo(attributes.get(i).glType()).componentCount() * 4;
        }
        // A location-less/zero-attribute shader (rare, but real - e.g. a shader with only builtin
        // gl_VertexID usage) would leave stride at 0; a 1-byte buffer avoids a zero-size allocation.
        int bufferSize = Math.max(stride, 1);

        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        ByteBuffer zeroed = MemoryUtil.memCalloc(bufferSize);
        try {
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, zeroed, GL15.GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(zeroed);
        }

        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        for (int i = 0; i < attributes.size(); i++) {
            AttributeInfo attr = attributes.get(i);
            if (attr.location() < 0) {
                continue;
            }
            ComponentInfo info = componentInfo(attr.glType());
            GL20.glEnableVertexAttribArray(attr.location());
            if (info.isInteger()) {
                GL30.glVertexAttribIPointer(attr.location(), info.componentCount(), GL11.GL_INT, stride, offsets[i]);
            } else {
                GL20.glVertexAttribPointer(attr.location(), info.componentCount(), GL11.GL_FLOAT, false, stride, offsets[i]);
            }
        }

        GL30.glBindVertexArray(0);
        return new VertexFormatSynthesizer(vao, vbo);
    }

    private record ComponentInfo(int componentCount, boolean isInteger) {
    }

    private static ComponentInfo componentInfo(int glType) {
        if (glType == GL20.GL_FLOAT) {
            return new ComponentInfo(1, false);
        } else if (glType == GL20.GL_FLOAT_VEC2) {
            return new ComponentInfo(2, false);
        } else if (glType == GL20.GL_FLOAT_VEC3) {
            return new ComponentInfo(3, false);
        } else if (glType == GL20.GL_FLOAT_VEC4) {
            return new ComponentInfo(4, false);
        } else if (glType == GL20.GL_INT) {
            return new ComponentInfo(1, true);
        } else if (glType == GL20.GL_INT_VEC2) {
            return new ComponentInfo(2, true);
        } else if (glType == GL20.GL_INT_VEC3) {
            return new ComponentInfo(3, true);
        } else if (glType == GL20.GL_INT_VEC4) {
            return new ComponentInfo(4, true);
        }
        // Defensive fallback for an attribute type not seen in any real shader so far (e.g. a
        // matrix-typed vertex input) - treats it as a single float rather than crashing. Flagged
        // here rather than silently correct: a real occurrence would under-allocate a matrix
        // attribute's true multi-location footprint.
        return new ComponentInfo(1, false);
    }

    @Override
    public void close() {
        GL30.glBindVertexArray(0);
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
    }
}
