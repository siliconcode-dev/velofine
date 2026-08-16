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

package dev.velofine.core.gui.mixin;

import dev.velofine.core.crash.CrashRecovery;
import dev.velofine.core.gui.ConfigKeyBinding;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Polls Velofine's own config keybind at the end of each client tick, and (Phase 9) marks the
 * crash-recovery session healthy on the first tick that ever runs.
 *
 * <p>A pure side-effect {@code @Inject} at {@code RETURN}, which is the lowest-risk mixin category
 * there is: whatever the handler does, it cannot change what {@code tick()} itself did. All the
 * actual logic lives in {@link ConfigKeyBinding}/{@link CrashRecovery} so nothing has to be merged
 * into {@code Minecraft}.
 *
 * <p>Optimus has its own separate mixin on this same method (the tick profiler and governor).
 * Two configs injecting into one target method is fine - Mixin merges them - and keeping them
 * apart is what lets the config screen stay reachable when Optimus is switched off.
 */
@Mixin(targets = "net.minecraft.client.Minecraft")
public abstract class MinecraftKeybindMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void velofine$pollConfigKeybind(CallbackInfo ci) {
        ConfigKeyBinding.poll((Minecraft) (Object) this);
        // Piggybacks on this same tick hook rather than adding a new mixin - the first tick to run
        // at all is well past GL-context/mixin-init startup risk. CrashRecovery.markSessionHealthy
        // is idempotent (guarded internally), so calling it every tick costs one CAS check.
        CrashRecovery.markSessionHealthy();
    }
}
