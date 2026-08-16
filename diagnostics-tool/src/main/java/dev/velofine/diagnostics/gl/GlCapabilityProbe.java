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

import dev.velofine.diagnostics.model.GlCapabilities;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GLCapabilities;

/**
 * Dumps GL_MAX_* limits and UBO/VAO/multi-draw-indirect support flags from the current context -
 * directly useful for confirming or ruling out the v1.5 root-cause hypothesis (Masterdoc_v1.5.md
 * S2: modern uniform-buffer/vertex-array rendering triggering real Intel driver bugs), captured
 * broadly rather than narrowed to a single suspected number. Not unit tested: requires a real
 * GPU/driver context.
 */
public final class GlCapabilityProbe {

    private GlCapabilityProbe() {
    }

    public static GlCapabilities probe() {
        GLCapabilities caps = GL.getCapabilities();

        int maxUniformBufferBindings = caps.OpenGL31 ? GL11.glGetInteger(GL31.GL_MAX_UNIFORM_BUFFER_BINDINGS) : -1;
        int maxUniformBlockSize = caps.OpenGL31 ? GL11.glGetInteger(GL31.GL_MAX_UNIFORM_BLOCK_SIZE) : -1;
        int maxVertexAttribs = GL11.glGetInteger(GL20.GL_MAX_VERTEX_ATTRIBS);
        int maxCombinedTextureImageUnits = GL11.glGetInteger(GL20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS);
        int maxArrayTextureLayers = caps.OpenGL30 ? GL11.glGetInteger(GL30.GL_MAX_ARRAY_TEXTURE_LAYERS) : -1;
        int maxDrawBuffers = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);

        boolean supportsUbo = caps.OpenGL31 || caps.GL_ARB_uniform_buffer_object;
        boolean supportsVao = caps.OpenGL30 || caps.GL_ARB_vertex_array_object;
        boolean supportsMultiDrawIndirect = caps.GL_ARB_multi_draw_indirect;

        return new GlCapabilities(
                maxUniformBufferBindings, maxUniformBlockSize, maxVertexAttribs, maxCombinedTextureImageUnits,
                maxArrayTextureLayers, maxDrawBuffers, supportsUbo, supportsVao, supportsMultiDrawIndirect);
    }
}
