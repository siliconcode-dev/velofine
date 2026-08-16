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

import java.nio.ByteBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

/**
 * A small, fixed-size (64x64) RGBA8 offscreen framebuffer (renderbuffer color attachment, no
 * depth needed for a survives-the-draw test) for {@code DrawCallTester}'s draw-call reproduction.
 *
 * <p>{@link #readPixelsRgba()} uses a plain synchronous {@code glReadPixels}, deliberately not a
 * PBO/fence-based async readback: for a single one-off 64x64 readback per shader (not a real-time
 * per-frame loop), the async pattern's throughput benefit doesn't apply, and the process-isolation
 * watchdog (see the pipeline package) already covers the "this call might hang" risk a blocking
 * readback would otherwise pose - adding async complexity here would duplicate a safety net that
 * already exists at the right layer. Not unit tested: requires a real GL context.
 */
public final class OffscreenFramebuffer implements AutoCloseable {

    public static final int SIZE = 64;

    private final int framebuffer;
    private final int colorRenderbuffer;

    private OffscreenFramebuffer(int framebuffer, int colorRenderbuffer) {
        this.framebuffer = framebuffer;
        this.colorRenderbuffer = colorRenderbuffer;
    }

    public static OffscreenFramebuffer create() {
        int renderbuffer = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, renderbuffer);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_RGBA8, SIZE, SIZE);

        int framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER, renderbuffer);

        return new OffscreenFramebuffer(framebuffer, renderbuffer);
    }

    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
        GL11.glViewport(0, 0, SIZE, SIZE);
    }

    public boolean checkStatus() {
        return GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE;
    }

    /** Reads the full RGBA8 pixel data back to a heap array ({@code SIZE*SIZE*4} bytes). */
    public byte[] readPixelsRgba() {
        ByteBuffer buffer = MemoryUtil.memAlloc(SIZE * SIZE * 4);
        try {
            GL11.glReadPixels(0, 0, SIZE, SIZE, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
            byte[] result = new byte[buffer.remaining()];
            buffer.get(result);
            return result;
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    @Override
    public void close() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL30.glDeleteFramebuffers(framebuffer);
        GL30.glDeleteRenderbuffers(colorRenderbuffer);
    }
}
