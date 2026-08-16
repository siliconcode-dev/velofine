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

package dev.velofine.shaders.format.blending;

import dev.velofine.core.log.VelofineLog;
import org.lwjgl.opengl.GL11;

import java.util.Optional;

/**
 * One of the four factors in a {@code blend.<pass>=<srcRgb> <dstRgb> <srcAlpha> <dstAlpha>}
 * directive. Adapted from IrisShaders/Iris ({@code common/.../gl/blending/BlendModeFunction.java}),
 * LGPL-3.0.
 */
public enum BlendModeFunction {
    ZERO(GL11.GL_ZERO),
    ONE(GL11.GL_ONE),
    SRC_COLOR(GL11.GL_SRC_COLOR),
    ONE_MINUS_SRC_COLOR(GL11.GL_ONE_MINUS_SRC_COLOR),
    DST_COLOR(GL11.GL_DST_COLOR),
    ONE_MINUS_DST_COLOR(GL11.GL_ONE_MINUS_DST_COLOR),
    SRC_ALPHA(GL11.GL_SRC_ALPHA),
    ONE_MINUS_SRC_ALPHA(GL11.GL_ONE_MINUS_SRC_ALPHA),
    DST_ALPHA(GL11.GL_DST_ALPHA),
    ONE_MINUS_DST_ALPHA(GL11.GL_ONE_MINUS_DST_ALPHA),
    SRC_ALPHA_SATURATE(GL11.GL_SRC_ALPHA_SATURATE);

    private final int glId;

    BlendModeFunction(int glId) {
        this.glId = glId;
    }

    public static Optional<BlendModeFunction> fromString(String name) {
        try {
            return Optional.of(BlendModeFunction.valueOf(name));
        } catch (IllegalArgumentException e) {
            VelofineLog.warn("Shaders", "Invalid blend mode function: " + name);
            return Optional.empty();
        }
    }

    public int glId() {
        return glId;
    }
}
