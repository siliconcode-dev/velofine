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

package dev.velofine.shaders.format;

/**
 * A parsed {@code size.buffer.<pass>=<x> <y>} directive - each axis is either an absolute pixel
 * size or a fraction of the main framebuffer's size (distinguished by whether the value contains a
 * decimal point, matching OptiFine's own convention). Adapted from IrisShaders/Iris
 * ({@code common/.../gl/texture/TextureScaleOverride.java}), LGPL-3.0.
 */
public final class TextureScaleOverride {
    public final boolean xRelative;
    public final boolean yRelative;
    public final float relativeX;
    public final float relativeY;
    public final int absoluteX;
    public final int absoluteY;

    public TextureScaleOverride(String xValue, String yValue) {
        if (xValue.contains(".")) {
            relativeX = Float.parseFloat(xValue);
            xRelative = true;
            absoluteX = 0;
        } else {
            absoluteX = Integer.parseInt(xValue);
            xRelative = false;
            relativeX = 0;
        }

        if (yValue.contains(".")) {
            relativeY = Float.parseFloat(yValue);
            yRelative = true;
            absoluteY = 0;
        } else {
            absoluteY = Integer.parseInt(yValue);
            yRelative = false;
            relativeY = 0;
        }
    }

    public int resolveX(int mainFramebufferWidth) {
        return xRelative ? Math.round(mainFramebufferWidth * relativeX) : absoluteX;
    }

    public int resolveY(int mainFramebufferHeight) {
        return yRelative ? Math.round(mainFramebufferHeight * relativeY) : absoluteY;
    }
}
