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
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets {@code net.minecraft.client.renderer.fog.FogRenderer.setupFog(...)} (confirmed real via
 * javap against the real 26.2 client jar - disassembly confirms the method's last four bytecode
 * instructions compute {@code renderDistanceEnd = renderDistance * 16} and
 * {@code renderDistanceStart = renderDistanceEnd - clamp(renderDistanceEnd / 10, 4, 64)} and store
 * them on the returned {@code FogData}). {@code FogData}'s fields are already real-{@code public}
 * (confirmed via javap - no fictitious-visibility trick needed, unlike
 * {@code OptionsSubScreen.list}).
 *
 * <p>{@code @Inject at RETURN} mutates the already-constructed {@code FogData} in place rather
 * than replacing it - when Fog is disabled, pushing both distances far past any real view distance
 * means the fog-distance falloff never triggers, without touching per-environment fog color/density
 * (rain, water, lava, powder snow), which come from the separate {@code FogEnvironment.setupFog}
 * polymorphic calls earlier in the same method and are left untouched.
 */
@Mixin(targets = "net.minecraft.client.renderer.fog.FogRenderer")
public abstract class FogRendererMixin {

    /** Far enough past any real render distance (32 chunks = 512 blocks) that fog never engages. */
    private static final float DISABLED_DISTANCE = 100_000.0f;

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void velofine$applyFogControl(Camera camera, int renderDistance, DeltaTracker deltaTracker,
            float partialTick, ClientLevel level, CallbackInfoReturnable<FogData> cir) {
        if (ConfigManager.get().utility.rendering.fogEnabled) {
            return;
        }
        FogData fogData = cir.getReturnValue();
        fogData.renderDistanceStart = DISABLED_DISTANCE;
        fogData.renderDistanceEnd = DISABLED_DISTANCE * 2;
    }
}
