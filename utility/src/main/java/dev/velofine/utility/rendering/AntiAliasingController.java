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

package dev.velofine.utility.rendering;

import dev.velofine.core.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.Identifier;

/**
 * Drives 26.2's own moddable post-processing asset mechanism ({@code GameRenderer.setPostEffect}/
 * {@code clearPostEffect}, confirmed real via javap - already used internally for blur/nausea/
 * creeper-flash effects, confirmed via the real {@code assets/minecraft/post_effect/*.json} bundled
 * in the client jar) to apply an FXAA pass, rather than a from-scratch renderer hack. No native
 * MSAA hook exists in 26.2's deferred-lighting pipeline, so this asset-level seam is the real one.
 *
 * <p><b>Known limitation, not yet resolved:</b> vanilla's post-effect slot is single-occupancy -
 * one active {@code Identifier} at a time, no stacking. This controller only (re)applies Velofine's
 * FXAA effect when the slot is currently empty, so it never clobbers a vanilla-triggered transient
 * effect (nether portal blur, creeper flash, nausea) while one is active - it resumes once vanilla
 * clears its own effect. This is a real trade-off, not a bug: while a vanilla effect is showing, AA
 * is briefly not applied, rather than Velofine fighting vanilla for the one slot every frame.
 *
 * <p><b>Also not yet resolved:</b> the {@code velofine:post/fxaa} and {@code velofine:fxaa}
 * identifiers this controller requests only resolve if something has registered a resource pack
 * exposing {@code assets/velofine/...} into the game's {@code ResourceManager} pack stack -
 * Velofine has never needed to ship a game asset before this feature, so that registration
 * mechanism does not exist yet. The JSON/GLSL assets are written and correct against the real
 * schema (see {@code utility/src/main/resources/assets/velofine/}), but until the pack-stack
 * registration piece is built, {@code setPostEffect} will silently fail to find them at runtime -
 * flagged explicitly rather than claimed as working.
 */
public final class AntiAliasingController {

    private static final Identifier FXAA_EFFECT = Identifier.fromNamespaceAndPath("velofine", "fxaa");

    private static boolean weApplied;

    private AntiAliasingController() {
    }

    public static void onTick(Minecraft minecraft) {
        boolean wantAa = ConfigManager.get().utility.rendering.antiAliasing;
        GameRenderer renderer = minecraft.gameRenderer;
        if (renderer == null) {
            return;
        }

        Identifier current = renderer.currentPostEffect();
        if (wantAa) {
            if (current == null) {
                renderer.setPostEffect(FXAA_EFFECT);
                weApplied = true;
            } else if (!FXAA_EFFECT.equals(current)) {
                // Vanilla (or something else) owns the slot right now - don't fight it.
                weApplied = false;
            }
        } else if (weApplied && FXAA_EFFECT.equals(current)) {
            renderer.clearPostEffect();
            weApplied = false;
        }
    }
}
