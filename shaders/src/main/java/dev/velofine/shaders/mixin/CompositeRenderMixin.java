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

import dev.velofine.shaders.pipeline.CompositeRenderer;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets {@code net.minecraft.client.renderer.GameRenderer.render(DeltaTracker, boolean)}
 * (confirmed real, once-per-frame - the same method Utility's {@code GameRenderMixin} already
 * injects into for {@code FrameTimeProfiler}, per {@code VerifyMixinsHarness}'s established
 * multiple-independent-injectors-on-one-method precedent). {@code @At("RETURN")}: pure side-effect
 * injection after the whole frame (world, entities, HUD) has already rendered - the lowest-risk
 * mixin category, the same one {@code MinecraftMixin}'s tick hooks and {@code GameRenderMixin}
 * itself already use. Mirrors the real call vanilla's own built-in post effects make at this same
 * point (confirmed via {@code -c} disassembly - see {@code CompositeRenderer}'s class javadoc).
 */
@Mixin(targets = "net.minecraft.client.renderer.GameRenderer")
public abstract class CompositeRenderMixin {

    @Inject(method = "render", at = @At("RETURN"))
    private void velofine$processCompositePipeline(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        CompositeRenderer.processFrame();
    }
}
