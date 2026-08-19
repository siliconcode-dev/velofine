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

package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.resources.Identifier;

/**
 * Confirmed real via javap against the real 26.2 jar (MC-308593 animated-texture-upload fix
 * research spike). {@code width()}/{@code height()}/{@code name()} are already real public
 * accessors on the real class - no {@code @Accessor} mixin needed for those; {@code byMipLevel}
 * stays unstubbed here since {@code SpriteContentsAccessor} exposes it directly via {@code @Accessor}
 * against the real class at runtime, independent of this stub.
 *
 * <p>{@link AnimatedTexture} is confirmed real as a genuine <em>non-static</em> inner class
 * (its real bytecode carries a synthetic {@code this$0} field) - but nothing in Velofine's own
 * source ever constructs one, only references the type (for the cast that recovers a real instance
 * from a mixin merged into it, and as {@link AnimationState}'s constructor's first parameter type),
 * so it is stubbed as an ordinary static nested class for simplicity; that difference from the real
 * class never surfaces since Velofine's code is never the one instantiating it.
 *
 * <p>{@link AnimationState}, by contrast, is confirmed real as a genuine <em>static</em> nested
 * class - its real constructor takes exactly three parameters with no synthetic outer-instance
 * argument (confirmed via javap, not merely inferred). Its real constructor is {@code private};
 * stubbed {@code public} here per the established
 * {@code core.gui.mixin.VideoSettingsScreenMixin}/{@code OptionsSubScreen} precedent (fictitious
 * stub visibility need not match the real declaration - once Mixin merges the calling code into a
 * real nestmate of the target class, real JVM nestmate-private access rules apply regardless of
 * what this stub declares).
 */
public class SpriteContents {

    public int width() {
        throw new UnsupportedOperationException("stub");
    }

    public int height() {
        throw new UnsupportedOperationException("stub");
    }

    public Identifier name() {
        throw new UnsupportedOperationException("stub");
    }

    public static final class AnimatedTexture {
    }

    public static final class AnimationState {

        public AnimationState(AnimatedTexture animationInfo, Int2ObjectMap<GpuTextureView> frameTexturesByIndex,
                GpuBufferSlice[] spriteUbosByMip) {
        }
    }
}
