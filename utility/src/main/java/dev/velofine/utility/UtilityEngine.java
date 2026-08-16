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

package dev.velofine.utility;

import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.core.mixin.MixinBridge;
import dev.velofine.shaders.ShaderEngine;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import java.lang.instrument.Instrumentation;

/**
 * Entry point for the Utility engine.
 *
 * <p>Phase 5 brought this engine into existence with nothing to inject - the master toggle, the
 * config panel, and {@link SafeDefaultsPolicy}, but no {@code mixins.utility.json}. Phase 6 ships
 * the first real Utility mixins (Zoom's {@code CameraMixin}, Fog's {@code FogRendererMixin}), so
 * this gains the same {@code MixinBootstrap.init()} / {@code Mixins.addConfiguration(...)} /
 * {@code MixinBridge.install(...)} sequence every other engine already uses - guarded by the same
 * engine-toggle check and log+disable-on-failure try/catch as {@code OptimusEngine}.
 */
public final class UtilityEngine {

    private UtilityEngine() {
    }

    public static void onAgentAttached(Instrumentation instrumentation) {
        if (!ConfigManager.get().engines.utility) {
            VelofineLog.info("Utility", "Engine disabled in config.");
            return;
        }
        try {
            // MixinBootstrap.init() and MixinBridge.install() are idempotent - safe to call here
            // regardless of whether CoreEngine/LegacySupportEngine/OptimusEngine already did.
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.utility.json", (IMixinConfigSource) null);
            MixinBridge.install(instrumentation);

            VelofineLog.info("Utility", "mixins active: zoom, fog control. Safe-defaults policy: "
                    + (SafeDefaultsPolicy.shouldStartSafe() ? "starting SAFE on this hardware" : "starting FULL"));
        } catch (Throwable t) {
            VelofineLog.warn("Utility", "Failed to initialize Mixin pipeline; Utility disabled: " + t);
            t.printStackTrace();
        }

        // Shaders (Phase 7) is a Utility Engine feature per Masterdoc 4.3 - no separate
        // engines.* toggle, gated by its own working.utility.shader.enabled instead. Runs
        // regardless of whether the mixin pipeline above succeeded, since it doesn't need
        // mixins.utility.json itself (see ShaderEngine's own javadoc).
        try {
            ShaderEngine.onAgentAttached(instrumentation);
        } catch (Throwable t) {
            VelofineLog.warn("Utility", "Failed to initialize shader pipeline: " + t);
            t.printStackTrace();
        }
    }
}
