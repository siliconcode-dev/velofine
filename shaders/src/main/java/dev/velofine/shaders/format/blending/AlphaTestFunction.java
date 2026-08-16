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

import org.lwjgl.opengl.GL11;

import java.util.Optional;

/**
 * The {@code alphaTest.<pass>=<function> <reference>} directive's function half. Adapted from
 * IrisShaders/Iris ({@code common/.../gl/blending/AlphaTestFunction.java}), LGPL-3.0.
 */
public enum AlphaTestFunction {
    NEVER(GL11.GL_NEVER),
    LESS(GL11.GL_LESS),
    EQUAL(GL11.GL_EQUAL),
    LEQUAL(GL11.GL_LEQUAL),
    GREATER(GL11.GL_GREATER),
    NOTEQUAL(GL11.GL_NOTEQUAL),
    GEQUAL(GL11.GL_GEQUAL),
    ALWAYS(GL11.GL_ALWAYS);

    private final int glId;

    AlphaTestFunction(int glId) {
        this.glId = glId;
    }

    public static Optional<AlphaTestFunction> fromString(String name) {
        if ("GL_ALWAYS".equals(name)) {
            return Optional.of(ALWAYS);
        }
        try {
            return Optional.of(AlphaTestFunction.valueOf(name));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public int glId() {
        return glId;
    }
}
