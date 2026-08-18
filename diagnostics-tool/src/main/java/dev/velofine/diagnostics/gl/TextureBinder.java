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

import dev.velofine.diagnostics.mc.ShaderExtractor;
import dev.velofine.diagnostics.model.UniformInfo;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

/**
 * Uploads real texture data once per pipeline run and binds it to whichever sampler uniforms a
 * program declares, before the draw test - before this class existed, no texture was ever bound at
 * all (confirmed: an exhaustive grep across the module found no {@code glBindTexture}/
 * {@code glTexImage2D} anywhere), so a shader whose fragment logic discards/blackens output based on
 * real sampled texture content could never be told apart from one that doesn't.
 *
 * <p>Prefers the real {@code water_still}/{@code lava_still} block textures extracted from the
 * client jar (directly relevant to the invisible-lava/water bug this tool exists to diagnose) for
 * samplers whose name suggests that's what they expect; every other sampler gets a small procedural
 * checkerboard (real, non-trivial texture data - not a flat color a shader could accidentally
 * "pass" against by coincidence) as a generic fallback, including when jar extraction fails for any
 * reason. Not unit tested: requires a real GL context.
 */
public final class TextureBinder implements AutoCloseable {

    private static final int CHECKERBOARD_SIZE = 8;

    private final int waterTexture;
    private final int lavaTexture;
    private final int fallbackTexture;

    private TextureBinder(int waterTexture, int lavaTexture, int fallbackTexture) {
        this.waterTexture = waterTexture;
        this.lavaTexture = lavaTexture;
        this.fallbackTexture = fallbackTexture;
    }

    public static TextureBinder create(ShaderExtractor extractor) {
        return new TextureBinder(
                loadOrFallback(extractor, "water_still"),
                loadOrFallback(extractor, "lava_still"),
                createCheckerboardTexture());
    }

    /** Caller must have already called {@code glUseProgram(program)}. */
    public void bindSamplers(List<UniformInfo> uniforms) {
        int unit = 0;
        for (UniformInfo uniform : uniforms) {
            if (uniform.location() < 0 || !isSampler(uniform.glType())) {
                continue;
            }
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, pickTexture(uniform.name()));
            GL20.glUniform1i(uniform.location(), unit);
            unit++;
        }
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }

    private int pickTexture(String samplerName) {
        String lower = samplerName.toLowerCase();
        if (lower.contains("water")) {
            return waterTexture;
        } else if (lower.contains("lava")) {
            return lavaTexture;
        }
        return fallbackTexture;
    }

    private static boolean isSampler(int glType) {
        return glType == GL20.GL_SAMPLER_2D || glType == GL20.GL_SAMPLER_CUBE || glType == GL20.GL_SAMPLER_3D;
    }

    private static int loadOrFallback(ShaderExtractor extractor, String textureName) {
        try {
            Optional<byte[]> png = extractor.readBlockTexturePng(textureName);
            if (png.isPresent()) {
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(png.get()));
                if (image != null) {
                    return uploadRgba(image.getWidth(), image.getHeight(), toRgbaBytes(image));
                }
            }
        } catch (IOException ignored) {
            // Falls through to the generic checkerboard below - a missing/unreadable real texture
            // isn't itself a finding worth surfacing, the fallback still gives a non-trivial sample.
        }
        return createCheckerboardTexture();
    }

    private static byte[] toRgbaBytes(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        byte[] rgba = new byte[width * height * 4];
        for (int i = 0; i < argb.length; i++) {
            int pixel = argb[i];
            rgba[i * 4] = (byte) ((pixel >> 16) & 0xFF);
            rgba[i * 4 + 1] = (byte) ((pixel >> 8) & 0xFF);
            rgba[i * 4 + 2] = (byte) (pixel & 0xFF);
            rgba[i * 4 + 3] = (byte) ((pixel >> 24) & 0xFF);
        }
        return rgba;
    }

    private static int createCheckerboardTexture() {
        int size = CHECKERBOARD_SIZE;
        byte[] rgba = new byte[size * size * 4];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                byte v = (byte) (((x + y) % 2 == 0) ? 220 : 40);
                int i = (y * size + x) * 4;
                rgba[i] = v;
                rgba[i + 1] = v;
                rgba[i + 2] = v;
                rgba[i + 3] = (byte) 255;
            }
        }
        return uploadRgba(size, size, rgba);
    }

    private static int uploadRgba(int width, int height, byte[] rgba) {
        int texture = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        ByteBuffer buffer = MemoryUtil.memAlloc(rgba.length);
        try {
            buffer.put(rgba).flip();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        } finally {
            MemoryUtil.memFree(buffer);
        }
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        return texture;
    }

    @Override
    public void close() {
        GL11.glDeleteTextures(waterTexture);
        GL11.glDeleteTextures(lavaTexture);
        GL11.glDeleteTextures(fallbackTexture);
    }
}
