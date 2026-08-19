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

package dev.velofine.legacysupport.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.NativeImage;
import dev.velofine.core.hardware.Fix;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.legacysupport.LegacySupportEngine;
import dev.velofine.legacysupport.texture.AnimatedTextureUploadFix;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Targets {@code SpriteContents$AnimatedTexture.createAnimationState(GpuBufferSlice, int)}
 * (confirmed real via javap against the real 26.2 jar - public method, non-static inner class of
 * {@code SpriteContents}). Fixes MC-308593 (see {@link AnimatedTextureUploadFix}'s class javadoc
 * for the full root cause and confirmed-via-javap signature notes) by cancelling vanilla's own
 * staging-buffer upload and substituting the old synchronous re-upload instead, only when
 * {@link Fix#ANIMATED_TEXTURE_UPLOAD_FIX} is active.
 *
 * <p>{@code this$0}/{@code uniqueFrames}/{@code frameRowSize} are all confirmed real fields
 * declared directly on {@code AnimatedTexture} itself (not inherited from a superclass), so plain
 * {@code @Shadow} applies cleanly here - none of the {@code @Shadow}-doesn't-walk-hierarchy
 * limitation (SpongePowered/Mixin#284, see {@code VideoSettingsScreenMixin}'s javadoc) is in play.
 *
 * <p>{@code SpriteContents.AnimationState}'s real constructor takes no synthetic outer-instance
 * argument (confirmed via javap - it's a genuine static nested class), so it's constructed directly
 * here with ordinary {@code new SpriteContents.AnimationState(...)} syntax against the mcstub's
 * matching constructor - this code is merged by Mixin into the real {@code AnimatedTexture} class,
 * a real nestmate of {@code AnimationState} under the same enclosing {@code SpriteContents}, so real
 * JVM nestmate-private access applies at the real merged bytecode regardless of the real
 * constructor's {@code private} declaration (same {@code OptionsSubScreen}-cast precedent
 * {@code VideoSettingsScreenMixin} already established - deliberately not the raw-reflection
 * approach the reference community mod needed, since that mod's mixin class is a separate, unrelated
 * class with no such access).
 */
@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$AnimatedTexture")
public abstract class SpriteContentsAnimatedTextureMixin {

    @Shadow
    @Final
    private SpriteContents this$0;

    @Shadow
    @Final
    private IntList uniqueFrames;

    @Shadow
    @Final
    private int frameRowSize;

    @Inject(method = "createAnimationState", at = @At("HEAD"), cancellable = true)
    private void velofine$fixAnimationUpload(GpuBufferSlice uboSlice, int spriteUboSize,
            CallbackInfoReturnable<SpriteContents.AnimationState> cir) {
        if (!LegacySupportEngine.isFixActive(Fix.ANIMATED_TEXTURE_UPLOAD_FIX)) {
            return;
        }

        try {
            SpriteContentsAccessor accessor = (SpriteContentsAccessor) (Object) this.this$0;
            NativeImage[] byMipLevel = accessor.velofine$getByMipLevel();

            AnimatedTextureUploadFix.Result result = AnimatedTextureUploadFix.buildFrameTextures(
                    byMipLevel, this.this$0.width(), this.this$0.height(), this.this$0.name(),
                    this.uniqueFrames, this.frameRowSize, uboSlice, spriteUboSize);

            SpriteContents.AnimatedTexture self = (SpriteContents.AnimatedTexture) (Object) this;
            cir.setReturnValue(new SpriteContents.AnimationState(
                    self, result.frameTexturesByIndex(), result.spriteUbosByMip()));
        } catch (Throwable t) {
            VelofineLog.warn("LegacySupport", "animated texture upload fix failed, falling back to vanilla "
                    + "(staging-buffer path, MC-308593 may still reproduce): " + t);
        }
    }
}
