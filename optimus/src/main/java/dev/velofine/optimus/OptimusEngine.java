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
import dev.velofine.core.mixin.MixinBridge;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import java.lang.instrument.Instrumentation;

/**
 * Entry point for the Optimus engine.
 *
 * <p>Through Phase 4, Optimus's optimizations were unconditional - no hardware gating, matching
 * how Sodium/Lithium/C2ME behave. Phase 5 adds the master engine toggle Build_plan's exit
 * criterion asks for: with {@code engines.optimus} off, {@code onAgentAttached} returns before
 * installing any mixin, so the game runs exactly as it would with Optimus never having been
 * built. Individual features (thread-pool tuning, the goal-selector throttle, the governor) have
 * their own finer-grained toggles on top of that - see {@code mixins.optimus.json}'s handlers and
 * {@code dev.velofine.optimus.governor}.
 *
 * <p>Two separate entry points, called at two different points in the launch sequence:
 * <ul>
 *   <li>{@link #applyThreadPoolTuning()} - must run <em>before</em> vanilla's
 *       {@code net.minecraft.util.Util} class loads (which lazily creates its shared
 *       cores-aware {@code ForkJoinPool} the first time {@code backgroundExecutor()}/{@code
 *       ioPool()} is touched), so it's called from {@code Main.main()} before self-attach, the
 *       same ordering already proven safe by Phase 3's {@code velofine.gameDir} property.</li>
 *   <li>{@link #onAgentAttached(Instrumentation)} - called from {@code VelofineAgent} after
 *       self-attach, boots Mixin and installs {@code mixins.optimus.json}.</li>
 * </ul>
 */
public final class OptimusEngine {

    private static final int MIN_THREADS = 1;
    private static final int MAX_THREADS = 255;

    private OptimusEngine() {
    }

    /**
     * Makes vanilla's own {@code cores - 1} background-thread-pool formula (confirmed via javap
     * against {@code net.minecraft.util.Util.maxAllowedExecutorThreads()}) an explicit,
     * Optimus-owned decision by pinning it via vanilla's own supported
     * {@code -Dmax.bg.threads=<1-255>} override property, rather than relying on the implicit
     * default. This one pool backs chunk mesh building ({@code SectionRenderDispatcher}) and
     * world-gen/IO dispatch ({@code ChunkTaskDispatcher} et al.) simultaneously, so one explicit,
     * auditable value benefits all three - Optimus doesn't need to reimplement any of that
     * threading itself.
     *
     * <p>Called only when the {@code threadPoolTuning} toggle is on ({@code Main} checks before
     * calling). Two defensive changes from Phase 4: it no longer overwrites a value the user (or
     * something further up the launch chain) already put in {@code -Dmax.bg.threads}, and the
     * computed count is clamped to vanilla's own documented 1-255 range before being written -
     * this property is read directly by vanilla, so an out-of-range value would reach it unchecked
     * otherwise.
     */
    public static void applyThreadPoolTuning() {
        if (System.getProperty("max.bg.threads") != null) {
            VelofineLog.info("Optimus", "max.bg.threads already set externally; leaving it alone.");
            return;
        }
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int threadCount = Math.max(MIN_THREADS, Math.min(MAX_THREADS, availableProcessors - 1));
        System.setProperty("max.bg.threads", String.valueOf(threadCount));
        VelofineLog.info("Optimus", "background thread pool: " + threadCount + " thread(s) (availableProcessors="
                + availableProcessors + ")");
    }

    public static void onAgentAttached(Instrumentation instrumentation) {
        if (!ConfigManager.get().engines.optimus) {
            VelofineLog.info("Optimus", "Engine disabled in config; Optimus mixins not applied.");
            return;
        }
        try {
            // MixinBootstrap.init() and MixinBridge.install() are idempotent - safe to call here
            // regardless of whether LegacySupportEngine/CoreEngine already initialized them.
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.optimus.json", (IMixinConfigSource) null);
            MixinBridge.install(instrumentation);

            VelofineLog.info("Optimus", "mixins active: goal-selector tick throttle, tick-time profiler, "
                    + "performance governor");
        } catch (Throwable t) {
            VelofineLog.warn("Optimus", "Failed to initialize Mixin pipeline; Optimus disabled: " + t);
            t.printStackTrace();
        }
    }
}
