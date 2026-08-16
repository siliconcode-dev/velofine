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
 * Confirmed real via javap. {@code getPostChain(Identifier, java.util.Set)} only resolves configs
 * discovered from real resource-pack assets during a reload pass, which a dynamically-built
 * shader-pack {@link PostChainConfig} is not - so Velofine's code bypasses it and calls {@link
 * PostChain#load} directly instead, using this class only as the source of the real {@code
 * postChainProjection}/{@code postChainProjectionMatrixBuffer} instances (exposed via {@code
 * dev.velofine.shaders.mixin.ShaderManagerAccessor}, since both fields are private) - the same
 * dedicated screen-space projection vanilla's own post-effect passes use, as opposed to the
 * level's real 3D projection. {@link #getPostChain} itself is intentionally not stubbed since
 * nothing here calls it.
 */
public class ShaderManager {

    public static final class CompilationException extends Exception {
    }
}
