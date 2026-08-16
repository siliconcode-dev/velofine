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

package dev.velofine.shaders.mixin;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes {@code GameRenderer}'s private {@code resourcePool} field - the same allocator vanilla's
 * own built-in post effects pass to {@code PostChain.process}. Must be typed to the field's exact
 * real declared type ({@link CrossFrameResourcePool}, not the {@code GraphicsResourceAllocator}
 * interface it implements) - confirmed empirically via {@code VerifyMixinsHarness}: typing this to
 * the interface threw {@code InvalidAccessorException} ("No candidates were found") at Mixin apply
 * time, since {@code @Accessor} resolution matches the field's declared type exactly.
 */
@Mixin(targets = "net.minecraft.client.renderer.GameRenderer")
public interface GameRendererAccessor {

    @Accessor("resourcePool")
    CrossFrameResourcePool velofine$getResourcePool();
}
