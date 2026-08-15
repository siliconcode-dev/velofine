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

package dev.velofine.optimus;

import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.log.VelofineLog;
import dev.velofine.optimus.governor.MinecraftGovernorHost;
import dev.velofine.optimus.governor.PerformanceGovernor;
import dev.velofine.optimus.profiling.TickProfiler;

/**
 * Everything Optimus does once per client tick, behind one entry point so {@code MinecraftMixin}
 * stays a pair of one-line delegations.
 *
 * <p>The governor is built lazily on the first tick rather than at agent attach, because
 * {@link MinecraftGovernorHost} references {@code Minecraft} and loading that class before the
 * transformer is installed would silently disable every mixin targeting it.
 *
 * <p>Failures here are swallowed and reported once. A bug in a frame-rate heuristic must not be
 * able to take the client's tick loop down with it.
 */
public final class OptimusClientTick {

    /** Seconds of frame-rate history the governor averages over. */
    private static final int FPS_WINDOW_SECONDS = 5;

    private static PerformanceGovernor governor;
    private static boolean governorFailed;

    private OptimusClientTick() {
    }

    public static void onTickStart() {
        if (ConfigManager.get().optimus.tickProfiler) {
            TickProfiler.onTickStart();
        }
    }

    public static void onTickEnd() {
        TickProfiler.onTickEnd();

        if (governorFailed) {
            return;
        }
        try {
            if (governor == null) {
                governor = new PerformanceGovernor(new MinecraftGovernorHost(), FPS_WINDOW_SECONDS);
            }
            governor.onClientTick();
        } catch (Throwable t) {
            governorFailed = true;
            VelofineLog.warn("Optimus", "Performance governor disabled after an error: " + t);
            t.printStackTrace();
        }
    }
}
