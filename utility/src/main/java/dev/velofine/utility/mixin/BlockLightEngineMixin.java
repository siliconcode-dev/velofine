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

package dev.velofine.utility.mixin;

import dev.velofine.utility.light.DynamicLightsState;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets {@code net.minecraft.world.level.lighting.BlockLightEngine.getEmission(long, BlockState)}
 * (confirmed real, private method via javap against the real 26.2 client jar - the light engine's
 * per-position emission query, consulted every time it propagates block light). {@code @Inject at
 * RETURN, cancellable}: when a Dynamic Lights source is currently tracked at the queried packed
 * position, returns the greater of vanilla's own real emission and the tracked light level - never
 * lower than what the real block would emit anyway, and never persisted anywhere.
 */
@Mixin(targets = "net.minecraft.world.level.lighting.BlockLightEngine")
public abstract class BlockLightEngineMixin {

    @Inject(method = "getEmission", at = @At("RETURN"), cancellable = true)
    private void velofine$applyDynamicLight(long packedPos, BlockState state, CallbackInfoReturnable<Integer> cir) {
        int dynamic = DynamicLightsState.emissionAt(packedPos);
        if (dynamic > cir.getReturnValue()) {
            cir.setReturnValue(dynamic);
        }
    }
}
