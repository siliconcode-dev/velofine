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
import dev.velofine.diagnostics.model.DrawTestResult;
import dev.velofine.diagnostics.report.FramebufferPngWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * Reproduces an actual draw call for a real linked program against an already-bound VAO (see
 * {@link VertexFormatSynthesizer} - built once per program by the caller and shared with
 * {@link UboRoundTripTester}, not rebuilt per test; a real, confirmed bug caught via a live run
 * against this machine's actual GPU was each tester independently building its own VAO but never
 * binding it before drawing, which the driver correctly rejected with
 * {@code GL_INVALID_OPERATION: There is no VAO bound} - fixed by making VAO binding an explicit,
 * shared precondition both testers rely on instead of each silently assuming their own).
 *
 * <p><b>Success criterion is deliberately narrow: "the draw call completed without a new GL error
 * and the framebuffer stayed complete" - NOT "renders correctly in-game."</b> The synthesized
 * geometry is a single zero-filled point with no real game data or textures bound; this is a
 * robustness/crash test at the draw-call level, not a visual-correctness test. See
 * {@link DrawTestResult}'s own javadoc for the same caveat, and {@code ResultsScreen} for where
 * this gets surfaced to a human. Not unit tested: requires a real GL context.
 */
public final class DrawCallTester {

    private DrawCallTester() {
    }

    public static DrawTestResult run(
            int program, int vao, Path shaderDir, String shaderName, String stage, String variant,
            List<DebugMessage> debugMessages) {

        try (OffscreenFramebuffer fbo = OffscreenFramebuffer.create()) {
            fbo.bind();
            boolean complete = fbo.checkStatus();
            GlErrorChecker.checkAndRecord("FBO setup for " + shaderName + " [" + variant + "]", debugMessages);

            GL20.glUseProgram(program);
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_POINTS, 0, 1);
            GlErrorChecker.checkAndRecord("draw " + shaderName + " [" + variant + "]", debugMessages);
            boolean drawOk = GL11.glGetError() == GL11.GL_NO_ERROR;

            byte[] pixels = fbo.readPixelsRgba();
            GlErrorChecker.checkAndRecord("readback " + shaderName + " [" + variant + "]", debugMessages);

            String pngPath = null;
            try {
                pngPath = FramebufferPngWriter.write(shaderDir, shaderName, stage, variant, pixels, OffscreenFramebuffer.SIZE);
            } catch (IOException e) {
                debugMessages.add(new DebugMessage("draw-call-tester", "PNG_WRITE_FAILED", -1, "N/A",
                        shaderName + "." + stage + " [" + variant + "]: " + e));
            }

            GL30.glBindVertexArray(0);
            GL20.glUseProgram(0);
            return new DrawTestResult(complete, drawOk, summarize(pixels), pngPath);
        }
    }

    private static String summarize(byte[] rgba) {
        int size = OffscreenFramebuffer.SIZE;
        int centerIndex = ((size / 2) * size + (size / 2)) * 4;
        int r = rgba[centerIndex] & 0xFF;
        int g = rgba[centerIndex + 1] & 0xFF;
        int b = rgba[centerIndex + 2] & 0xFF;
        int a = rgba[centerIndex + 3] & 0xFF;
        return "center pixel rgba(" + r + "," + g + "," + b + "," + a + ")";
    }
}
