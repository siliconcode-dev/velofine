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

package dev.velofine.shaders;

import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.config.VelofineConfig;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.core.mixin.MixinBridge;
import dev.velofine.core.shader.ShaderPackBrowserRegistry;
import dev.velofine.core.shader.ShaderSourceInterceptors;
import dev.velofine.shaders.pipeline.CompositeShaderInterceptor;
import dev.velofine.shaders.pipeline.GbuffersShaderInterceptor;
import dev.velofine.shaders.pipeline.ShaderPipelineState;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;

/**
 * Entry point for the shader pipeline feature, called from {@code UtilityEngine} (Masterdoc 4.3
 * frames shaders as a Utility Engine feature - there is no separate {@code engines.*} toggle, see
 * {@code VelofineConfig.UtilitySection.ShaderSection}).
 *
 * <p>Registers a {@link ShaderPackBrowserRegistry} browser unconditionally (once a game directory
 * is known, regardless of whether the feature is enabled) so the Utility config page can list
 * available packs before a user has ever turned the feature on - pack discovery is cheap directory-
 * listing I/O, unlike actually loading one. Activates the configured pack (if any) so {@link
 * ShaderPipelineState} has something for {@link GbuffersShaderInterceptor}/{@link
 * CompositeShaderInterceptor} to consult, registers both with {@code core}'s always-installed
 * {@code GlDeviceMixin} redirect, and installs {@code mixins.shaders.json} (this module's first
 * real mixins) - only when a pack is actually selected, matching every other engine's "don't
 * install mixins for a feature nobody asked for" convention.
 */
public final class ShaderEngine {

    private ShaderEngine() {
    }

    public static void onAgentAttached(Instrumentation instrumentation) {
        String gameDirProperty = System.getProperty("velofine.gameDir");
        if (gameDirProperty == null) {
            VelofineLog.warn("Shaders", "No game directory known; cannot locate shaderpacks/.");
            return;
        }
        Path gameDir = Path.of(gameDirProperty);
        ShaderPackBrowserRegistry.register(new ShaderPackBrowserImpl(gameDir));

        VelofineConfig.ShaderSection config = ConfigManager.get().utility.shader;

        if (!config.enabled) {
            VelofineLog.info("Shaders", "Feature disabled in config.");
            return;
        }

        if (config.selectedPackName == null || config.selectedPackName.isBlank()) {
            VelofineLog.info("Shaders", "Enabled, but no pack selected.");
            return;
        }

        ShaderPipelineState.activate(gameDir, config.selectedPackName);
        ShaderSourceInterceptors.register(ShaderSourceInterceptors.PRIORITY_SHADER_PACK, new GbuffersShaderInterceptor());
        ShaderSourceInterceptors.register(ShaderSourceInterceptors.PRIORITY_SHADER_PACK, new CompositeShaderInterceptor());

        try {
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.shaders.json", (IMixinConfigSource) null);
            MixinBridge.install(instrumentation);
            VelofineLog.info("Shaders", "Composite pipeline mixins active.");
        } catch (Throwable t) {
            VelofineLog.warn("Shaders", "Failed to install composite pipeline mixins "
                    + "(gbuffers-only substitution still active): " + t);
            t.printStackTrace();
        }
    }
}
