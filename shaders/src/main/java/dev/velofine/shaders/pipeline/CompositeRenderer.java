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

package dev.velofine.shaders.pipeline;

import dev.velofine.core.log.VelofineLog;
import dev.velofine.shaders.mixin.GameRendererAccessor;
import dev.velofine.shaders.mixin.ShaderManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;

import java.util.Set;

/**
 * Loads and drives the real {@link PostChain} for the active shader pack's composite/deferred/
 * final chain, following the exact same real mechanism confirmed via {@code -c} disassembly of
 * vanilla's own {@code GameRenderer.render(...)}: {@code postChain.process(mainRenderTarget,
 * resourcePool)}, called once per frame. Bypasses {@code ShaderManager.getPostChain(Identifier,
 * Set)} (which only resolves configs from real, static resource-pack assets - see {@code
 * ShaderManager}'s own mcstub javadoc) in favor of calling {@code PostChain.load(...)} directly
 * with a dynamically-built {@link PostChainConfig}, reusing {@code ShaderManager}'s own dedicated
 * post-chain {@link Projection}/{@link ProjectionMatrixBuffer} instances via {@link
 * ShaderManagerAccessor} rather than the level's real 3D projection (semantically correct here -
 * composite/deferred/final passes are full-screen-quad operations over already-rendered textures,
 * not 3D geometry).
 *
 * <p><b>Loading is deliberately lazy</b>, deferred to the first {@link #processFrame()} call rather
 * than done in {@link #onPackActivated} directly: {@code ShaderPipelineState.activate} (and
 * therefore this class's activation) runs at agent-attach time, well before {@code
 * Minecraft.getInstance()}/{@code GameRenderer}/{@code ShaderManager} exist - those are only
 * constructed once vanilla's own startup reaches that point. {@link #processFrame()}, by
 * definition, only ever runs once real frames are actually being rendered, so real instances are
 * guaranteed to exist there.
 *
 * <p><b>Unverified in a real running game</b> - grounded in real javap/bytecode research, not yet
 * confirmed by an actual launch. If loading fails (a GLSL compile/link error in the pack's own
 * composite/deferred/final source, most likely), this logs and leaves no chain active - gbuffers-
 * only rendering still works via {@code GbuffersShaderInterceptor} regardless.
 */
public final class CompositeRenderer {

    private static volatile PostChainConfig pendingConfig;
    private static volatile boolean loadAttempted;
    private static volatile PostChain activeChain;

    private CompositeRenderer() {
    }

    public static void onPackActivated(PostChainConfig config) {
        close();
        pendingConfig = config;
        loadAttempted = false;
    }

    /** Called once per frame from {@code CompositeRenderMixin}. No-op if no pack is active, loading already failed, or the pack has no composite/deferred/final passes. */
    public static void processFrame() {
        if (!loadAttempted && pendingConfig != null) {
            loadAttempted = true;
            tryLoad(pendingConfig);
        }

        PostChain chain = activeChain;
        if (chain == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        GameRendererAccessor gameRendererAccessor = (GameRendererAccessor) mc.gameRenderer;
        chain.process(mc.gameRenderer.mainRenderTarget(), gameRendererAccessor.velofine$getResourcePool());
    }

    private static void tryLoad(PostChainConfig config) {
        try {
            Minecraft mc = Minecraft.getInstance();
            ShaderManagerAccessor shaderManagerAccessor = (ShaderManagerAccessor) mc.getShaderManager();
            Projection projection = shaderManagerAccessor.velofine$getPostChainProjection();
            ProjectionMatrixBuffer projectionMatrixBuffer = shaderManagerAccessor.velofine$getPostChainProjectionMatrixBuffer();

            activeChain = PostChain.load(config, mc.getTextureManager(), Set.of(PostChain.MAIN_TARGET_ID),
                    PostChain.MAIN_TARGET_ID, projection, projectionMatrixBuffer);

            VelofineLog.info("Shaders", "Composite/deferred/final pipeline loaded (" + config.passes().size() + " pass(es))");
        } catch (Throwable t) {
            VelofineLog.warn("Shaders", "Failed to load composite/deferred/final pipeline; "
                    + "falling back to gbuffers-only rendering: " + t);
        }
    }

    public static void close() {
        pendingConfig = null;
        loadAttempted = false;
        PostChain chain = activeChain;
        activeChain = null;
        if (chain != null) {
            chain.close();
        }
    }
}
