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

package dev.velofine.optimus.mixin;

import dev.velofine.optimus.profiling.TickProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets {@code net.minecraft.client.Minecraft.tick()} (confirmed real, single-overload method
 * via javap against the real vanilla 26.2 client jar) - vanilla's one client-tick entry point.
 * Pure {@code @Inject} (no {@code @Overwrite}/{@code @Redirect}/{@code @ModifyConstant}): adds
 * timing side-effects only, never alters tick behavior itself - the lowest-risk mixin category,
 * since it can't change game logic regardless of what {@link TickProfiler} does with the samples.
 */
@Mixin(targets = "net.minecraft.client.Minecraft")
public abstract class MinecraftMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void velofine$onTickStart(CallbackInfo ci) {
        TickProfiler.onTickStart();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void velofine$onTickEnd(CallbackInfo ci) {
        TickProfiler.onTickEnd();
    }
}
