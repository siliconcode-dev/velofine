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
 * type-aware data at driver-reported offsets, binds it, and confirms the draw survives.
 *
 * <p>Member offsets, types, and (for matrix members) column stride are all queried from the driver
 * ({@code glGetActiveUniformsiv(..., GL_UNIFORM_OFFSET/GL_UNIFORM_TYPE/GL_UNIFORM_MATRIX_STRIDE,
 * ...)}), never hand-computed via {@code std140} layout arithmetic - avoids the exact class of
 * self-inflicted bug (this tool's own layout-math mistake) that would masquerade as a driver bug.
 * Matrix members (mat2/mat3/mat4 - e.g. a {@code ChunkSection.ModelViewMat}) get a real identity
 * matrix written column-by-column at the driver-reported stride - the only value guaranteed not to
 * degenerate whatever transform it represents, regardless of what the matrix actually is. Every
 * other member gets a small, uniform non-zero fill across its own byte span (bounded by the next
 * member's offset, or the block's end) rather than the previous single-byte-pattern-for-the-whole-
 * block approach, which zeroed out this distinction entirely.
 *
 * <p><b>Honest scope</b> (see {@link UboRoundTripResult}'s javadoc for the full statement): this
 * confirms the block populates and binds without a GL error and the draw survives - it does
 * <b>not</b> confirm the shader's math actually consumed the values correctly, and non-matrix
 * members still aren't encoded per their exact declared type (float vs. int vs. vecN) - "plausible
 * non-zero," not "semantically exact." Not unit tested: requires a real linked program on a real GL
 * context.
 */
public final class UboRoundTripTester {

    /** Confirmed real block names this tool knows how to test - see the class javadoc. */
    public static final List<String> KNOWN_BLOCKS = List.of("Fog", "Globals", "ChunkSection");

    // Repeating bit pattern for the IEEE-754 float 1.0f, used as a plausible non-zero fill for
    // every non-matrix member's byte span - see the class javadoc for why this isn't type-exact.
    private static final byte[] NON_ZERO_WORD = {0x00, 0x00, (byte) 0x80, 0x3F};

    private UboRoundTripTester() {
    }

    private record MemberLayout(String name, int offset, int glType, int matrixStride) {
    }

    /**
     * Returns {@code null} if {@code blockName} isn't an active uniform block on this program.
     * {@code vao} must be a real VAO built for this program's attributes (see
     * {@code DrawCallTester}'s javadoc for why this is a required, shared parameter rather than
     * each tester building its own - {@code glDrawArrays} with no VAO bound is a real, confirmed
     * {@code GL_INVALID_OPERATION} this class hit before the fix). {@code uniforms}/{@code textures}
     * apply the same plausible-defaults/real-texture treatment {@code DrawCallTester} gives plain
     * uniforms, so a shader mixing block and non-block uniforms doesn't have its MVP degenerate via
     * the non-block half even while this test focuses on the block half.
     */
    public static UboRoundTripResult run(
            int program, int vao, String blockName,
            List<dev.velofine.diagnostics.model.UniformInfo> uniforms, TextureBinder textures,
            List<DebugMessage> debugMessages) {
        int blockIndex = GL31.glGetUniformBlockIndex(program, blockName);
        if (blockIndex == GL31.GL_INVALID_INDEX) {
            return null;
        }

        int dataSize = GL31.glGetActiveUniformBlocki(program, blockIndex, GL31.GL_UNIFORM_BLOCK_DATA_SIZE);
        List<MemberLayout> members = queryMemberLayout(program, blockIndex);
        Map<String, Integer> memberOffsets = new LinkedHashMap<>();
        members.forEach(m -> memberOffsets.put(m.name(), m.offset()));

        int ubo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, ubo);
        ByteBuffer data = MemoryUtil.memAlloc(Math.max(dataSize, 1));
        try {
            fillTypeAware(data, members, dataSize);
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
        UniformDefaults.apply(program, uniforms);
        textures.bindSamplers(uniforms);
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
            int program, int vao, List<UniformBlockInfo> activeBlocks,
            List<dev.velofine.diagnostics.model.UniformInfo> uniforms, TextureBinder textures,
            List<DebugMessage> debugMessages) {
        return KNOWN_BLOCKS.stream()
                .filter(name -> activeBlocks.stream().anyMatch(b -> b.name().equals(name)))
                .map(name -> run(program, vao, name, uniforms, textures, debugMessages))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private static void fillTypeAware(ByteBuffer data, List<MemberLayout> members, int dataSize) {
        for (int i = 0; i < members.size(); i++) {
            MemberLayout member = members.get(i);
            int spanEnd = (i + 1 < members.size()) ? members.get(i + 1).offset() : dataSize;

            int columns = matrixColumnCount(member.glType());
            if (columns > 0 && member.matrixStride() > 0) {
                int rowBytes = matrixRowCount(member.glType()) * 4;
                for (int col = 0; col < columns; col++) {
                    int colOffset = member.offset() + col * member.matrixStride();
                    // Identity: row == col gets 1.0f, every other row in this column stays 0
                    // (data buffer starts zero-initialized by memAlloc's caller not guaranteeing
                    // that - explicitly zero this column's row-major span first).
                    for (int b = 0; b < rowBytes && colOffset + b < data.capacity(); b++) {
                        data.put(colOffset + b, (byte) 0);
                    }
                    int diagonalByteOffset = colOffset + col * 4;
                    if (diagonalByteOffset + 4 <= data.capacity()) {
                        data.put(diagonalByteOffset, NON_ZERO_WORD[0]);
                        data.put(diagonalByteOffset + 1, NON_ZERO_WORD[1]);
                        data.put(diagonalByteOffset + 2, NON_ZERO_WORD[2]);
                        data.put(diagonalByteOffset + 3, NON_ZERO_WORD[3]);
                    }
                }
            } else {
                for (int offset = member.offset(); offset < spanEnd && offset < data.capacity(); offset++) {
                    data.put(offset, NON_ZERO_WORD[(offset - member.offset()) % 4]);
                }
            }
        }
    }

    private static int matrixColumnCount(int glType) {
        if (glType == GL20.GL_FLOAT_MAT4) {
            return 4;
        } else if (glType == GL20.GL_FLOAT_MAT3) {
            return 3;
        } else if (glType == GL20.GL_FLOAT_MAT2) {
            return 2;
        }
        return 0;
    }

    private static int matrixRowCount(int glType) {
        return matrixColumnCount(glType); // only square matrix types are handled specially
    }

    private static List<MemberLayout> queryMemberLayout(int program, int blockIndex) {
        List<MemberLayout> members = new java.util.ArrayList<>();
        int memberCount = GL31.glGetActiveUniformBlocki(program, blockIndex, GL31.GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer indices = stack.mallocInt(memberCount);
            GL31.glGetActiveUniformBlockiv(program, blockIndex, GL31.GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES, indices);

            IntBuffer offsetBuf = stack.mallocInt(memberCount);
            GL31.glGetActiveUniformsiv(program, indices, GL31.GL_UNIFORM_OFFSET, offsetBuf);

            IntBuffer typeBuf = stack.mallocInt(memberCount);
            GL31.glGetActiveUniformsiv(program, indices, GL31.GL_UNIFORM_TYPE, typeBuf);

            IntBuffer strideBuf = stack.mallocInt(memberCount);
            GL31.glGetActiveUniformsiv(program, indices, GL31.GL_UNIFORM_MATRIX_STRIDE, strideBuf);

            IntBuffer nameSize = stack.mallocInt(1);
            IntBuffer nameType = stack.mallocInt(1);
            List<MemberLayout> unsorted = new java.util.ArrayList<>();
            for (int i = 0; i < memberCount; i++) {
                // Same glGetActiveUniform(program, index, size, type)->String convenience form
                // ProgramIntrospector already uses - a block member is just a regular indexed
                // uniform, queryable the same way.
                String name = GL20.glGetActiveUniform(program, indices.get(i), nameSize, nameType);
                unsorted.add(new MemberLayout(name, offsetBuf.get(i), typeBuf.get(i), strideBuf.get(i)));
            }
            unsorted.stream().sorted(java.util.Comparator.comparingInt(MemberLayout::offset)).forEach(members::add);
        }
        return members;
    }
}
