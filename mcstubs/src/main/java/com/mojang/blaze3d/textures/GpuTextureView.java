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

package com.mojang.blaze3d.textures;

/**
 * Confirmed real via javap against the real 26.2 jar - an empty marker is enough here, since
 * Velofine's own code only ever passes {@code GpuTextureView} instances through (received from
 * {@code GpuDevice.createTextureView}, stored in the frame-texture map handed to
 * {@code SpriteContents.AnimationState}'s constructor), never calls methods on one directly.
 */
public abstract class GpuTextureView implements AutoCloseable {
}
