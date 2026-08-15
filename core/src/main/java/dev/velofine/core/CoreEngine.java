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
 */
public final class CoreEngine {

    private CoreEngine() {
    }

    public static void onAgentAttached(Instrumentation instrumentation) {
        try {
            // All three calls are idempotent and global per-JVM, so it does not matter whether an
            // engine got here first (see MixinBridge.install's guard).
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.velofine-core.json", (IMixinConfigSource) null);
            MixinBridge.install(instrumentation);

            VelofineLog.info("core", "config UI entry points active (video settings, pause menu, keybind)");
        } catch (Throwable t) {
            VelofineLog.warn("core", "Failed to install config UI entry points: " + t);
            t.printStackTrace();
        }
    }
}
