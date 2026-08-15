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

import dev.velofine.core.config.ConfigManager;
import dev.velofine.utility.zoom.ZoomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets {@code net.minecraft.client.Camera.calculateFov(float)} (confirmed real, private
 * method via javap against the real 26.2 client jar - disassembly confirms it reads
 * {@code minecraft.options.fov().get()} live every call). {@code Options.fov} is hard-clamped to
 * {@code IntRange(30, 110)} by vanilla's own {@code OptionInstance.set()} validation, so real
 * zoom-level FOVs (well under 30) cannot be reached by writing through the option - this
 * {@code @Inject at RETURN} overrides the already-computed value directly instead, after vanilla's
 * own death/fluid FOV modifiers have already been applied, exactly the same composition point
 * {@code modifyFovBasedOnDeathOrFluid} uses. Pure post-hoc override of a float return value - the
 * lowest-risk mixin category, same as every other {@code @Inject} in this codebase.
 */
@Mixin(targets = "net.minecraft.client.Camera")
public abstract class CameraMixin {

    @Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
    private void velofine$applyZoom(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (!ConfigManager.get().utility.zoom.enabled) {
            return;
        }
        double factor = ZoomState.currentFactor();
        if (factor <= 1.0) {
            return;
        }
        float baseFov = cir.getReturnValue();
        cir.setReturnValue((float) (baseFov / factor));
    }
}
