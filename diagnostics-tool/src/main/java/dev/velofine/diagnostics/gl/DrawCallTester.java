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
import dev.velofine.diagnostics.model.UniformInfo;
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
 * <p><b>{@code drawSucceededNoNewError} means the draw call completed without a new GL error and the
 * framebuffer stayed complete</b> - a crash/error/hang check, not by itself proof of visual
 * correctness (the synthesized geometry is still a single point, not real game geometry). As of
 * Fix 3 (v1.5 diagnostic-tool rework) this is no longer structurally blind to visual output, though:
 * the framebuffer is cleared to {@link OffscreenFramebuffer#SENTINEL_RGBA} before the draw, real
 * per-shader uniform defaults are set ({@link UniformDefaults}), real texture data is bound
 * ({@link TextureBinder}), and {@link #readAnyRenderedPixel} scans the whole buffer for the first
 * pixel that differs from the sentinel rather than assuming the untested center pixel is where the
 * single synthesized point happened to land. See {@link DrawTestResult}'s javadoc for how this
 * feeds {@code report.ReportComparator}'s golden-reference visual-regression diff. Not unit tested:
 * requires a real GL context.
 */
public final class DrawCallTester {

    private DrawCallTester() {
    }

    public static DrawTestResult run(
            int program, int vao, List<UniformInfo> uniforms, TextureBinder textures,
            Path shaderDir, String shaderName, String stage, String variant,
            List<DebugMessage> debugMessages) {

        try (OffscreenFramebuffer fbo = OffscreenFramebuffer.create()) {
            fbo.bind();
            boolean complete = fbo.checkStatus();
            fbo.clearToSentinel();
            GlErrorChecker.checkAndRecord("FBO setup for " + shaderName + " [" + variant + "]", debugMessages);

            GL20.glUseProgram(program);
            UniformDefaults.apply(program, uniforms);
            textures.bindSamplers(uniforms);
            GlErrorChecker.checkAndRecord("uniform/texture defaults for " + shaderName + " [" + variant + "]", debugMessages);

            // Best-effort: core-profile drivers generally clamp glPointSize to their aliased-point
            // range (often just 1px) unless the vertex shader itself writes gl_PointSize with
            // GL_PROGRAM_POINT_SIZE enabled, which real unmodified vanilla shaders don't do - this
            // can't be relied on alone, which is why the readback below scans the whole buffer
            // rather than assuming a large, guaranteed-covered point.
            GL11.glPointSize(OffscreenFramebuffer.SIZE);

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
                String context = shaderName + "." + stage + " [" + variant + "]";
                debugMessages.add(new DebugMessage("draw-call-tester", "PNG_WRITE_FAILED", -1, "N/A",
                        context + ": " + e, context));
            }

            GL30.glBindVertexArray(0);
            GL20.glUseProgram(0);

            int[] rendered = readAnyRenderedPixel(pixels);
            boolean anyPixelRendered = rendered != null;
            int[] sampledRgba = anyPixelRendered ? rendered : OffscreenFramebuffer.SENTINEL_RGBA;
            String summary = anyPixelRendered
                    ? "rendered pixel rgba(" + sampledRgba[0] + "," + sampledRgba[1] + "," + sampledRgba[2] + "," + sampledRgba[3] + ")"
                    : "nothing rasterized (framebuffer stayed at sentinel color)";

            return new DrawTestResult(complete, drawOk, anyPixelRendered, sampledRgba, summary, pngPath);
        }
    }

    /**
     * Scans the full buffer for the first pixel that differs from {@link OffscreenFramebuffer#SENTINEL_RGBA}
     * beyond a small tolerance (driver dithering/rounding, not a real color difference), returning
     * {@code null} if every pixel is still the sentinel - i.e. the single synthesized point never
     * actually rasterized into the buffer at all, a real and distinguishable finding in its own
     * right rather than being silently reported as if it were shader output.
     */
    private static int[] readAnyRenderedPixel(byte[] rgba) {
        int size = OffscreenFramebuffer.SIZE;
        int[] sentinel = OffscreenFramebuffer.SENTINEL_RGBA;
        for (int i = 0; i < size * size; i++) {
            int offset = i * 4;
            int r = rgba[offset] & 0xFF;
            int g = rgba[offset + 1] & 0xFF;
            int b = rgba[offset + 2] & 0xFF;
            int a = rgba[offset + 3] & 0xFF;
            if (Math.abs(r - sentinel[0]) > 2 || Math.abs(g - sentinel[1]) > 2
                    || Math.abs(b - sentinel[2]) > 2 || Math.abs(a - sentinel[3]) > 2) {
                return new int[] {r, g, b, a};
            }
        }
        return null;
    }
}
