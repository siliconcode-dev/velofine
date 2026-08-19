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

package dev.velofine.legacysupport.texture;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.velofine.core.log.VelofineLog;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.resources.Identifier;

/**
 * Workaround for Mojang's own regression MC-308593 (confirmed via web research plus a real, working
 * community Fabric mod, {@code colGem-ga/26.2-Invisible-Water-Fix}, MIT-licensed - read and adapted
 * this phase, not copied file-for-file): in 26.2 Pre-Release 3, animated-texture frame uploads
 * (water, lava, fire, soul fire, lanterns, sea lanterns) moved from the old synchronous
 * {@code CommandEncoder.writeToTexture(NativeImage, ...)} path to a GPU staging-buffer path
 * ({@code transientMemory().multiUploadStaging(...)} + {@code copyBufferToTexture(...)}). Intel
 * Gen7 drivers (HD Graphics 4000/2500) don't correctly honor {@code GL_UNPACK_IMAGE_HEIGHT}/
 * row-stride during that staging-buffer sub-region copy, so frame data uploads from the wrong
 * offset - landing as fully transparent (water) or solid black (lava/fire/lanterns), while the
 * animation's timing/geometry stays correct (matches live tester confirmation on the i5-3470S/HD
 * Graphics 2500 reference machine this phase). Static, non-animated textures never go through this
 * path and are unaffected.
 *
 * <p>This re-implements the old per-frame upload directly: for each unique animation frame and mip
 * level, crop that frame out of the full mip-level sprite-sheet {@link NativeImage} and upload it
 * with the synchronous {@code writeToTexture} path instead of the broken staging-buffer one. Pure/
 * static, following {@code shader.ShaderPatcher}'s precedent as a plain helper class separate from
 * the mixin that calls it - takes only plain data, no Mixin-specific types beyond the real Blaze3D
 * API surface every call here already needs.
 */
public final class AnimatedTextureUploadFix {

    private AnimatedTextureUploadFix() {
    }

    /**
     * @param byMipLevel    the sprite's full, un-cropped image for each mip level (real field:
     *                      {@code SpriteContents.byMipLevel}, via {@code SpriteContentsAccessor}).
     * @param frameRowSize  how many frames are laid out per row in the sprite sheet (real field:
     *                      {@code AnimatedTexture.frameRowSize}).
     * @param uboSlice      the shared UBO slice vanilla's own {@code createAnimationState} received -
     *                      unrelated to the broken upload path, passed straight through unmodified.
     */
    public static Result buildFrameTextures(NativeImage[] byMipLevel, int width, int height, Identifier name,
            IntList uniqueFrames, int frameRowSize, GpuBufferSlice uboSlice, int spriteUboSize) {
        GpuDevice device = RenderSystem.getDevice();
        CommandEncoder encoder = device.createCommandEncoder();
        Int2ObjectMap<GpuTextureView> frameTexturesByIndex = new Int2ObjectOpenHashMap<>();

        for (int i = 0; i < uniqueFrames.size(); i++) {
            int frame = uniqueFrames.getInt(i);
            int frameIndex = frame;
            GpuTexture texture = device.createTexture(() -> name + " velofine animation frame " + frameIndex,
                    GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST, GpuFormat.RGBA8_UNORM,
                    width, height, 1, byMipLevel.length);

            // Same offset formula the real (working, pre-26.2) upload path used, and the one the
            // real MC-308593 community fix independently re-derived and confirmed working - frames
            // are laid out left-to-right, top-to-bottom in the sprite sheet, frameRowSize wide.
            int offsetX = (frame % frameRowSize) * width;
            int offsetY = (frame / frameRowSize) * height;

            for (int level = 0; level < byMipLevel.length; level++) {
                NativeImage fullImage = byMipLevel[level];
                int frameW = width >> level;
                int frameH = height >> level;
                int srcOffsetX = offsetX >> level;
                int srcOffsetY = offsetY >> level;

                try (NativeImage cropped = new NativeImage(frameW, frameH, false)) {
                    fullImage.copyRect(cropped, srcOffsetX, srcOffsetY, 0, 0, frameW, frameH, false, false);
                    encoder.writeToTexture(texture, cropped, level, 0, 0, 0);
                }
            }

            frameTexturesByIndex.put(frame, device.createTextureView(texture));
        }

        GpuBufferSlice[] spriteUbosByMip = new GpuBufferSlice[byMipLevel.length];
        for (int level = 0; level < byMipLevel.length; level++) {
            spriteUbosByMip[level] = uboSlice.slice((long) level * spriteUboSize, spriteUboSize);
        }

        VelofineLog.info("LegacySupport", "animated texture upload fix: re-uploaded " + uniqueFrames.size()
                + " frame(s) for " + name + " via the synchronous path (MC-308593 workaround)");
        return new Result(frameTexturesByIndex, spriteUbosByMip);
    }

    public record Result(Int2ObjectMap<GpuTextureView> frameTexturesByIndex, GpuBufferSlice[] spriteUbosByMip) {
    }
}
