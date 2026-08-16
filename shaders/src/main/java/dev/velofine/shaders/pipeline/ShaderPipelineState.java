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

import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.shaders.discovery.ShaderpackDiscovery;
import dev.velofine.shaders.discovery.ShaderpackHandle;
import dev.velofine.shaders.format.ShaderPack;
import net.minecraft.client.renderer.PostChainConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * The single currently-active shader pack (or none), read by {@code GbuffersShaderRedirectMixin}
 * on every shader compilation. A restart-required setting today - {@code UtilityEngine} calls
 * {@link #activate}/{@link #deactivate} once at agent-attach time from config, matching every other
 * install-time mixin-config decision in this codebase (per CLAUDE.md's Phase 5 "Applies" liveness
 * convention) - swapping packs mid-session without restarting is future work for the options UI
 * (7.7), not built here.
 *
 * <p>Owns the active pack's {@link ShaderpackHandle} lifecycle: zip-mounted packs need their
 * filesystem closed exactly once, when Velofine stops using them, not per-file-read.
 */
public final class ShaderPipelineState {

    private static volatile ShaderPack activePack;
    private static volatile ShaderpackHandle activeHandle;
    private static volatile GbuffersProgramMapping mapping;
    private static volatile PostChainConfig activeCompositeConfig;

    private ShaderPipelineState() {
    }

    public static synchronized void activate(Path gameDir, String packName) {
        deactivate();

        ShaderpackDiscovery discovery = new ShaderpackDiscovery(gameDir);
        try {
            ShaderpackHandle handle = ShaderpackHandle.open(discovery.getShaderpacksDirectory(), packName);
            Map<String, String> overrides = ConfigManager.get().utility.shader.packOptions.getOrDefault(packName, Map.of());
            ShaderPack pack = ShaderPack.load(packName, handle.getRoot(), overrides);
            activeHandle = handle;
            activePack = pack;
            activeCompositeConfig = CompositePipelineBuilder.build(pack);
            CompositeRenderer.onPackActivated(activeCompositeConfig);
            VelofineLog.info("Shaders", "Activated shader pack: " + packName
                    + (activeCompositeConfig != null
                            ? " (" + activeCompositeConfig.passes().size() + " composite/deferred/final pass(es))"
                            : " (gbuffers-only, no composite/deferred/final passes found)"));
        } catch (IOException e) {
            VelofineLog.warn("Shaders", "Failed to activate shader pack " + packName + ": " + e);
        }
    }

    public static synchronized void deactivate() {
        if (activeHandle != null) {
            try {
                activeHandle.close();
            } catch (IOException e) {
                VelofineLog.warn("Shaders", "Failed to close previous shader pack: " + e);
            }
        }
        activeHandle = null;
        activePack = null;
        activeCompositeConfig = null;
        CompositeProgramRegistry.clear();
        CompositeRenderer.close();
    }

    public static ShaderPack getActivePack() {
        return activePack;
    }

    /**
     * {@code null} for a gbuffers-only pack (no {@code deferred}/{@code composite}/{@code final}
     * programs found). Built once at {@link #activate}; consuming it to actually drive a real
     * {@code PostChain.load()}/{@code .process()} render-loop hook is the remaining 7.5 work - see
     * CLAUDE.md's Phase 7 notes.
     */
    public static PostChainConfig getActiveCompositeConfig() {
        return activeCompositeConfig;
    }

    /**
     * Built on first use, not at class-load time - {@link GbuffersProgramMapping}'s constructor
     * reads real {@code RenderPipelines} static fields, which only needs those vanilla classes to
     * be loaded (true well before any shader ever compiles), not any particular Velofine
     * initialization order.
     */
    public static GbuffersProgramMapping getMapping() {
        GbuffersProgramMapping local = mapping;
        if (local == null) {
            synchronized (ShaderPipelineState.class) {
                local = mapping;
                if (local == null) {
                    local = new GbuffersProgramMapping();
                    mapping = local;
                }
            }
        }
        return local;
    }
}
