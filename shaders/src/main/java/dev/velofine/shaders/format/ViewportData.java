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
 * A parsed {@code scale.<pass>=<scale> [offsetX offsetY]} directive. Adapted from IrisShaders/Iris
 * ({@code common/.../gl/framebuffer/ViewportData.java}), LGPL-3.0.
 */
public record ViewportData(float scale, float offsetX, float offsetY) {
    public static final ViewportData DEFAULT = new ViewportData(1.0f, 0.0f, 0.0f);
}
