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
import dev.velofine.core.config.VelofineConfig;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets {@code net.minecraft.client.renderer.SectionOcclusionGraph.isInViewDistance(long, long)}
 * (confirmed real, private method via javap against the real 26.2 jar - {@code -c} disassembly
 * confirms it is a purely horizontal (X/Z) check, delegating to
 * {@code ChunkTrackingView.isInViewDistance(viewDistance, centerX, centerZ, x, z)} with no Y
 * component at all - vanilla has no vertical render-distance concept to begin with).
 *
 * <p>{@code @Inject at RETURN, cancellable}: when a section already fails the horizontal check,
 * vanilla's own {@code false} is left alone. When it passes horizontally and Variable Render
 * Distance is enabled, this additionally requires the section's Y to be within
 * {@code verticalDistance} sections of the reference position (read via the real
 * {@code SectionPos.x/y/z(long)} static accessors {@code isInViewDistance} itself never touches).
 *
 * <p><b>Scope note:</b> this only affects occlusion-graph visibility, not {@code ViewArea}'s
 * section allocation (world height bounds) - out-of-vertical-range sections still technically
 * exist and can still be mesh-built, just never marked visible. Correct end-user behaviour (you
 * do not see chunks beyond N sections above/below you), at the cost of not saving the mesh-build
 * work those sections would otherwise cost - a deliberate v1 trade-off against touching
 * {@code ViewArea}'s more invasive section-allocation shape.
 */
@Mixin(targets = "net.minecraft.client.renderer.SectionOcclusionGraph")
public abstract class RenderDistanceMixin {

    @Inject(method = "isInViewDistance", at = @At("RETURN"), cancellable = true)
    private void velofine$applyVerticalDistance(long reference, long target, CallbackInfoReturnable<Boolean> cir) {
        VelofineConfig.RenderDistanceSection section = ConfigManager.get().utility.renderDistance;
        if (!section.enabled || section.verticalDistance <= 0 || !cir.getReturnValue()) {
            return;
        }
        int deltaY = Math.abs(SectionPos.y(reference) - SectionPos.y(target));
        if (deltaY > section.verticalDistance) {
            cir.setReturnValue(false);
        }
    }
}
