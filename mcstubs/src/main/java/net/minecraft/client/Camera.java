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

package net.minecraft.client;

/**
 * Signature-only. {@code CameraMixin} targets {@code calculateFov(float)} - a private method
 * resolved by Mixin against the real class bytecode via {@code @Mixin(targets = "...")}, not via
 * this stub - so no members are declared here; the type only needs to exist so {@code Camera}
 * can appear as a parameter type in {@code FogRendererMixin}'s injected method signature
 * ({@code FogRenderer.setupFog}'s first parameter, confirmed via javap against the real 26.2 jar).
 */
public class Camera {
}
