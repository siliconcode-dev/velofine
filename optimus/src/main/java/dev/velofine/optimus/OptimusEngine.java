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

import dev.velofine.core.log.VelofineLog;
import dev.velofine.core.mixin.MixinBridge;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;

import java.lang.instrument.Instrumentation;

/**
 * Entry point for the Optimus engine. Unlike {@code LegacySupportEngine}, Optimus's optimizations
 * are unconditional - they apply on every install regardless of detected hardware, matching how
 * Sodium/Lithium/C2ME actually behave (no hardware gating; a per-engine on/off toggle is Phase 5,
 * not this phase). There is no {@code Fix}/{@code HardwareProfile} indirection here as a result -
 * every mixin in {@code mixins.optimus.json} just always applies.
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
     */
    public static void applyThreadPoolTuning() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int threadCount = Math.max(1, availableProcessors - 1);
        System.setProperty("max.bg.threads", String.valueOf(threadCount));
        VelofineLog.info("Optimus", "background thread pool: " + threadCount + " thread(s) (availableProcessors="
                + availableProcessors + ")");
    }

    public static void onAgentAttached(Instrumentation instrumentation) {
        try {
            // MixinBootstrap.init() and MixinBridge.install() are idempotent - safe to call here
            // regardless of whether LegacySupportEngine already initialized them (it only does so
            // when at least one hardware-gated fix is active; Optimus always needs Mixin booted).
            MixinBootstrap.init();
            Mixins.addConfiguration("mixins.optimus.json", (IMixinConfigSource) null);
            MixinBridge.install(instrumentation);

            VelofineLog.info("Optimus", "mixins active: goal-selector tick throttle, tick-time profiler");
        } catch (Throwable t) {
            VelofineLog.warn("Optimus", "Failed to initialize Mixin pipeline; Optimus disabled: " + t);
            t.printStackTrace();
        }
    }
}
