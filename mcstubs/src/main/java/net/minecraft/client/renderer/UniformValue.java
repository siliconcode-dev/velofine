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

package net.minecraft.client.renderer;

/**
 * Confirmed real via javap - a custom per-pass uniform value {@link PostChainConfig.Pass} can
 * declare. Referenced only as a type parameter by Velofine's code today (no custom uniform
 * expressions are constructed yet - {@code ShaderProperties}' own javadoc tracks that as deferred
 * scope), so nothing beyond the bare type is stubbed.
 */
public interface UniformValue {
}
