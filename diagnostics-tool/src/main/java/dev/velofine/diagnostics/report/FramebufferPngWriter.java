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

package dev.velofine.diagnostics.report;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Converts a {@code DrawCallTester}/{@code OffscreenFramebuffer} RGBA readback into a real PNG
 * file - literal visual proof of what a shader actually rendered on a given machine, saved next to
 * the report. Deliberately uses JDK {@code ImageIO} rather than adding an image-writing LWJGL
 * dependency (e.g. {@code lwjgl-stb}) - consistent with this module's minimal-dependency posture.
 */
public final class FramebufferPngWriter {

    private FramebufferPngWriter() {
    }

    /**
     * Writes {@code <shaderDir>/<name>.<stage>.<variant>.png}. {@code rgba} must be
     * {@code size*size*4} bytes, row-major starting from the bottom row - real
     * {@code glReadPixels} output is bottom-up, so this flips vertically to produce a
     * conventionally-oriented (top-down) PNG.
     */
    public static String write(Path shaderDir, String shaderName, String stage, String variant, byte[] rgba, int size)
            throws IOException {
        Files.createDirectories(shaderDir);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < size; y++) {
            int sourceRow = size - 1 - y;
            for (int x = 0; x < size; x++) {
                int i = (sourceRow * size + x) * 4;
                int r = rgba[i] & 0xFF;
                int g = rgba[i + 1] & 0xFF;
                int b = rgba[i + 2] & 0xFF;
                int a = rgba[i + 3] & 0xFF;
                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }

        String fileName = shaderName + "." + stage + "." + variant + ".png";
        Path target = shaderDir.resolve(fileName);
        ImageIO.write(image, "png", target.toFile());
        return shaderDir.getFileName() + "/" + fileName;
    }
}
