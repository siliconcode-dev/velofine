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

package dev.velofine.core;

import dev.velofine.core.log.VelofineLog;
import dev.velofine.core.mixin.MixinBridge;
import dev.velofine.core.updater.UpdateOrchestrator;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import java.lang.instrument.Instrumentation;

/**
 * Installs the three mixins that put a "Velofine" entry point into vanilla's UI.
 *
 * <p>These live in {@code core} rather than in any engine on purpose: the config screen must stay
 * reachable even when <em>every</em> engine has been switched off, otherwise a user who disables
 * all three has locked themselves out of the only place to switch them back on. Unlike
 * {@code LegacySupportEngine}, this therefore has no enable gate at all.
 *
 * <p><strong>Do not touch any {@code dev.velofine.core.gui} class from here.</strong> Those classes
 * extend real Minecraft types, so class-loading one during agent attach would pull
 * {@code Screen}/{@code Minecraft} in <em>before</em> {@link MixinBridge#install} has registered
 * the transformer - and every mixin targeting an already-loaded class silently does nothing. The
 * UI classes are only reached when a screen is actually opened, long after that.
 *
 * <p>Also installs {@code mixins.core-shader.json} (just {@code GlDeviceMixin}, Phase 7) - the
 * shared shader-source redirect every engine's GLSL interception layers onto via {@link
 * dev.velofine.core.shader.ShaderSourceInterceptors}. Unlike the GUI mixins, this one has nothing
 * to do with the config screen; it lives here purely because it is the one place guaranteed to
 * install regardless of which individual engines are enabled - see {@code
 * ShaderSourceInterceptors}' class javadoc for why that matters.
 *
 * <p>Phase 8: also kicks off the passive, rate-limited update check here. Safe unlike a {@code
 * core.gui} reference would be - {@link UpdateOrchestrator#checkIfDueOnLaunch()} and everything it
 * calls into ({@code core.updater}) is pure HTTP/IO/Gson with zero Minecraft-type references, so
 * it never risks the "class-loaded Screen/Minecraft before the transformer is installed" trap
 * described above. It also just submits to a background thread and returns immediately, so it
 * cannot delay game startup.
 */
public final class CoreEngine {

    private CoreEngine() {
    }

    public static void onAgentAttached(Instrumentation instrumentation) {
        try {
            // All calls are idempotent and global per-JVM, so it does not matter whether an
            // engine got here first (see MixinBridge.install's guard).
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.velofine-core.json", (IMixinConfigSource) null);
            Mixins.addConfiguration("mixins.core-shader.json", (IMixinConfigSource) null);
            MixinBridge.install(instrumentation);

            VelofineLog.info("core", "config UI entry points active (video settings, pause menu, keybind, "
                    + "title screen branding); shader source redirect active");
        } catch (Throwable t) {
            VelofineLog.warn("core", "Failed to install config UI entry points: " + t);
            t.printStackTrace();
        }

        try {
            UpdateOrchestrator.reportIfJustApplied();
            UpdateOrchestrator.checkIfDueOnLaunch();
        } catch (Throwable t) {
            VelofineLog.warn("core", "Update check could not start: " + t);
        }
    }
}
