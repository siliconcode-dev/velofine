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

package dev.velofine.core.shader;

import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * One engine's opinion on what a real GLSL source lookup should return, registered with {@link
 * ShaderSourceInterceptors}. Implementations must check their own active/enabled state internally
 * and return {@link Optional#empty()} when not interested - registration itself is unconditional
 * (see {@link ShaderSourceInterceptors}'s class javadoc for why).
 */
@FunctionalInterface
public interface ShaderSourceInterceptor {

    Optional<String> intercept(Identifier id, ShaderType type, String vanillaSource);
}
