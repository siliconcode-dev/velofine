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

package dev.velofine.optimus.profiling;

import dev.velofine.core.log.VelofineLog;

/**
 * Lightweight client-tick timing sampler, driven by {@code MinecraftMixin}'s {@code @Inject}s at
 * the head/return of {@code Minecraft.tick()}. Deliberately simple (raw {@link System#nanoTime()}
 * sampling over a rolling window, not a full JFR custom-event framework): there's no
 * reference-hardware profiling data to design a heavier tool around yet, so this exists to give
 * testers and future phases (this phase's own tuning constants, Phase 5's adaptive governor) real
 * numbers instead of guesses - "find and address bottlenecks" per Build_plan's Phase 4 goal can
 * only mean "apply well-precedented community techniques and instrument for future tuning" without
 * that data, same honesty pattern as Phases 2/3's "hypothesis, not confirmed" framing.
 *
 * <p>Always called from the client (render) thread by {@code Minecraft.tick()} itself, so plain
 * static fields are correct here - no cross-thread access, no need for volatile/atomics.
 *
 * <p>Since Phase 5 the profiler is off by default and toggleable at runtime, so {@link #onTickEnd}
 * can be reached without a matching {@link #onTickStart} - the toggle may have flipped mid-tick.
 * The {@code sampling} latch makes every sample a genuine start/end pair rather than measuring
 * from whenever the profiler last happened to be enabled.
 */
public final class TickProfiler {

    private static final int SAMPLE_WINDOW_TICKS = 100;

    private static long tickStartNanos;
    private static boolean sampling;
    private static long minNanos = Long.MAX_VALUE;
    private static long maxNanos;
    private static long totalNanos;
    private static int sampleCount;

    private TickProfiler() {
    }

    public static void onTickStart() {
        tickStartNanos = System.nanoTime();
        sampling = true;
    }

    public static void onTickEnd() {
        if (!sampling) {
            return;
        }
        sampling = false;

        long elapsedNanos = System.nanoTime() - tickStartNanos;
        minNanos = Math.min(minNanos, elapsedNanos);
        maxNanos = Math.max(maxNanos, elapsedNanos);
        totalNanos += elapsedNanos;
        sampleCount++;

        if (sampleCount >= SAMPLE_WINDOW_TICKS) {
            logSummary();
            reset();
        }
    }

    private static void logSummary() {
        double avgMs = (totalNanos / (double) sampleCount) / 1_000_000.0;
        double minMs = minNanos / 1_000_000.0;
        double maxMs = maxNanos / 1_000_000.0;
        VelofineLog.info("Optimus", String.format(java.util.Locale.ROOT,
                "tick time over last %d ticks: min=%.2fms avg=%.2fms max=%.2fms", sampleCount, minMs, avgMs, maxMs));
    }

    private static void reset() {
        minNanos = Long.MAX_VALUE;
        maxNanos = 0;
        totalNanos = 0;
        sampleCount = 0;
    }
}
