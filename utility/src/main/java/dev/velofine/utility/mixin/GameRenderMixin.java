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
import dev.velofine.utility.profiling.FrameTimeProfiler;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets {@code net.minecraft.client.renderer.GameRenderer.render(DeltaTracker, boolean)}
 * (confirmed real, once-per-rendered-frame method via javap against the real 26.2 client jar).
 * Pure side-effect {@code @Inject at HEAD} - the lowest-risk mixin category.
 */
@Mixin(targets = "net.minecraft.client.renderer.GameRenderer")
public abstract class GameRenderMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void velofine$onFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (ConfigManager.get().utility.fpsOverlay.enabled) {
            FrameTimeProfiler.onFrame();
        }
    }
}
