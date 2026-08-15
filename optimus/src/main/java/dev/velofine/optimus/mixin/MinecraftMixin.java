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

import dev.velofine.optimus.OptimusClientTick;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets {@code net.minecraft.client.Minecraft.tick()} (confirmed real, single-overload method
 * via javap against the real vanilla 26.2 client jar) - vanilla's one client-tick entry point.
 * Pure {@code @Inject} (no {@code @Overwrite}/{@code @Redirect}/{@code @ModifyConstant}): adds
 * side-effects only, never alters tick behavior itself - the lowest-risk mixin category, since it
 * can't change game logic regardless of what {@link OptimusClientTick} does.
 *
 * <p>Since Phase 5 this drives both the tick profiler and the performance governor. Core has its
 * own separate mixin on this same method for the config keybind; two mixin configs injecting into
 * one target method is fine (Mixin merges them), and keeping them apart is what lets the config
 * screen stay reachable when Optimus is switched off entirely.
 */
@Mixin(targets = "net.minecraft.client.Minecraft")
public abstract class MinecraftMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void velofine$onTickStart(CallbackInfo ci) {
        OptimusClientTick.onTickStart();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void velofine$onTickEnd(CallbackInfo ci) {
        OptimusClientTick.onTickEnd();
    }
}
