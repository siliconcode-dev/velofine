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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Rebrands the vanilla "Minecraft &lt;version&gt;" corner watermark to "Velofine &lt;version&gt;".
 *
 * <p>Confirmed via javap against the real 26.2 client jar: {@code TitleScreen.extractRenderState}
 * (the 26.2 render-state-extraction override, not an immediate-mode {@code render(...)} - see
 * CLAUDE.md's Phase 5 finding on this) builds the watermark inline as {@code "Minecraft " +
 * SharedConstants.getCurrentVersion().name()} via an {@code invokedynamic} string-concat call, then
 * stores it into one local variable slot that gets conditionally re-concatenated twice more (a demo
 * suffix, then a "modded" suffix) before being drawn via {@code GuiGraphicsExtractor.text(...)}.
 * The concat itself is an {@code invokedynamic} call site (not a plain {@code ldc} String
 * constant), so {@code @ModifyConstant} cannot target it directly - {@code @ModifyVariable}
 * intercepting the local right after its <em>first</em> store is the correct seam instead: both
 * later re-concatenations read and rebuild from that same local, so rewriting only the first
 * assignment correctly rebrands every downstream variant (demo/modded included) without needing to
 * intercept them separately.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.TitleScreen")
public abstract class TitleScreenMixin {

    private static final String VANILLA_PREFIX = "Minecraft ";
    private static final String VELOFINE_PREFIX = "Velofine ";

    @ModifyVariable(method = "extractRenderState", at = @At(value = "STORE", ordinal = 0), ordinal = 0)
    private String velofine$rebrandVersionText(String value) {
        return value.startsWith(VANILLA_PREFIX) ? VELOFINE_PREFIX + value.substring(VANILLA_PREFIX.length()) : value;
    }
}
