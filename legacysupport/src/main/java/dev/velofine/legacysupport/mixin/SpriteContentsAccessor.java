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

package dev.velofine.legacysupport.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code SpriteContents.byMipLevel} (confirmed real via javap against the real 26.2 jar -
 * the one field of the four MC-308593's animated-texture-upload fix needs that has no existing
 * public accessor; {@code width()}/{@code height()}/{@code name()} are already real public methods
 * and are called directly, no {@code @Accessor} needed for those). Same lowest-risk
 * {@code @Accessor}-only-generates-a-getter pattern already established by
 * {@code shaders.mixin.ShaderManagerAccessor}/{@code GameRendererAccessor}.
 */
@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents")
public interface SpriteContentsAccessor {

    @Accessor("byMipLevel")
    NativeImage[] velofine$getByMipLevel();
}
