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

import dev.velofine.diagnostics.model.DebugMessage;
import dev.velofine.diagnostics.model.UboRoundTripResult;
import dev.velofine.diagnostics.model.UniformBlockInfo;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Populates one of the three confirmed real {@code std140} uniform blocks ({@code Fog}/
 * {@code Globals}/{@code ChunkSection} - exact member lists verified against real 26.2 bytes) with
 * sentinel data at driver-reported offsets, binds it, and confirms the draw survives.
 *
 * <p>Member offsets are queried from the driver ({@code glGetActiveUniformsiv(..., GL_UNIFORM_OFFSET,
 * ...)}), never hand-computed via {@code std140} layout arithmetic - avoids the exact class of
 * self-inflicted bug (this tool's own layout-math mistake) that would masquerade as a driver bug.
 *
 * <p><b>Honest scope</b> (see {@link UboRoundTripResult}'s javadoc for the full statement): this
 * confirms the block populates and binds without a GL error and the draw survives - it does
 * <b>not</b> confirm the shader's math actually consumed the values correctly. The sentinel pattern
 * fills the block's entire driver-reported byte range uniformly rather than type-aware
 * per-member encoding (a mat4 member like {@code ChunkSection.ModelViewMat} would need
 * {@code GL_UNIFORM_MATRIX_STRIDE}-aware column packing to write real matrix data) - consistent
 * with not claiming to verify shader-side consumption anyway. Not unit tested: requires a real
 * linked program on a real GL context.
 */
public final class UboRoundTripTester {

    /** Confirmed real block names this tool knows how to test - see the class javadoc. */
    public static final List<String> KNOWN_BLOCKS = List.of("Fog", "Globals", "ChunkSection");

    private static final byte SENTINEL_BYTE = 0x3F; // arbitrary non-zero fill pattern

    private UboRoundTripTester() {
    }

    /**
     * Returns {@code null} if {@code blockName} isn't an active uniform block on this program.
     * {@code vao} must be a real VAO built for this program's attributes (see
     * {@code DrawCallTester}'s javadoc for why this is a required, shared parameter rather than
     * each tester building its own - {@code glDrawArrays} with no VAO bound is a real, confirmed
     * {@code GL_INVALID_OPERATION} this class hit before the fix).
     */
    public static UboRoundTripResult run(int program, int vao, String blockName, List<DebugMessage> debugMessages) {
        int blockIndex = GL31.glGetUniformBlockIndex(program, blockName);
        if (blockIndex == GL31.GL_INVALID_INDEX) {
            return null;
        }

        int dataSize = GL31.glGetActiveUniformBlocki(program, blockIndex, GL31.GL_UNIFORM_BLOCK_DATA_SIZE);
        Map<String, Integer> memberOffsets = queryMemberOffsets(program, blockIndex);

        int ubo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, ubo);
        ByteBuffer data = MemoryUtil.memAlloc(Math.max(dataSize, 1));
        try {
            for (int i = 0; i < data.capacity(); i++) {
                data.put(i, SENTINEL_BYTE);
            }
            GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, data, GL15.GL_DYNAMIC_DRAW);
        } finally {
            MemoryUtil.memFree(data);
        }

        // GLSL 330 blocks (no explicit layout(binding=...)) require the application to assign a
        // binding point at runtime - binding point 0 is arbitrary but consistent per-call.
        int bindingPoint = 0;
        GL31.glUniformBlockBinding(program, blockIndex, bindingPoint);
        GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, bindingPoint, ubo);
        GlErrorChecker.checkAndRecord("UBO populate/bind for block " + blockName, debugMessages);
        boolean populatedOk = GL11.glGetError() == GL11.GL_NO_ERROR;

        GL20.glUseProgram(program);
        GL30.glBindVertexArray(vao);
        GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);
        GlErrorChecker.checkAndRecord("draw with UBO block " + blockName + " bound", debugMessages);
        boolean drawSurvived = GL11.glGetError() == GL11.GL_NO_ERROR;
        GL30.glBindVertexArray(0);
        GL20.glUseProgram(0);

        GL15.glDeleteBuffers(ubo);

        return new UboRoundTripResult(blockName, bindingPoint, memberOffsets, populatedOk, drawSurvived);
    }

    /** Convenience: runs every {@link #KNOWN_BLOCKS} entry that's actually active on this program. */
    public static List<UboRoundTripResult> runAllKnownBlocks(
            int program, int vao, List<UniformBlockInfo> activeBlocks, List<DebugMessage> debugMessages) {
        return KNOWN_BLOCKS.stream()
                .filter(name -> activeBlocks.stream().anyMatch(b -> b.name().equals(name)))
                .map(name -> run(program, vao, name, debugMessages))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static Map<String, Integer> queryMemberOffsets(int program, int blockIndex) {
        Map<String, Integer> offsets = new LinkedHashMap<>();
        int memberCount = GL31.glGetActiveUniformBlocki(program, blockIndex, GL31.GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer indices = stack.mallocInt(memberCount);
            GL31.glGetActiveUniformBlockiv(program, blockIndex, GL31.GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES, indices);

            IntBuffer offsetBuf = stack.mallocInt(memberCount);
            GL31.glGetActiveUniformsiv(program, indices, GL31.GL_UNIFORM_OFFSET, offsetBuf);

            IntBuffer nameSize = stack.mallocInt(1);
            IntBuffer nameType = stack.mallocInt(1);
            for (int i = 0; i < memberCount; i++) {
                // Same glGetActiveUniform(program, index, size, type)->String convenience form
                // ProgramIntrospector already uses - a block member is just a regular indexed
                // uniform, queryable the same way.
                String name = GL20.glGetActiveUniform(program, indices.get(i), nameSize, nameType);
                offsets.put(name, offsetBuf.get(i));
            }
        }
        return offsets;
    }
}
