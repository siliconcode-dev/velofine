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

package dev.velofine.utility.profiling;

import dev.velofine.core.status.LiveStatus;

import java.util.Arrays;
import java.util.Locale;

/**
 * Per-frame timing sampler, modeled on the modern FPS-overlay class of tool researched this
 * session (avg FPS, 1%/0.1% lows, frame time - richer than vanilla F3's raw counter, in line with
 * 2026's current standard for this feature). Driven by a {@code @Inject at HEAD} on
 * {@code GameRenderer.render(DeltaTracker, boolean)} (confirmed real, once-per-frame method via
 * javap), the same rolling-window {@code System.nanoTime()} approach {@code TickProfiler} already
 * established for ticks - no JFR, deliberately simple.
 *
 * <p>Surfaced via {@link LiveStatus} rather than an in-world HUD draw: 26.2's persistent gameplay
 * HUD render pass ({@code Gui.extractRenderState}) does not hand out a {@code GuiGraphicsExtractor}
 * the way a {@code Screen}'s own widget extraction loop does (confirmed via javap - its signature
 * takes no such parameter), and reverse-engineering that separate pipeline was out of scope for
 * this pass - flagged rather than guessed at. The config panel's live readout (the same mechanism
 * {@code OptimusPage} already uses for the governor) is a real, working surface today.
 */
public final class FrameTimeProfiler {

    private static final int CAPACITY = 512;

    private static final long[] samples = new long[CAPACITY];
    private static int sampleCount;
    private static int nextIndex;
    private static long lastFrameNanos;

    private FrameTimeProfiler() {
    }

    /** Package-visible for {@code FrameTimeProfilerTest} - static state otherwise leaks across test methods. */
    static void resetForTest() {
        sampleCount = 0;
        nextIndex = 0;
        lastFrameNanos = 0;
        LiveStatus.setFps("fps: --");
    }

    public static void onFrame() {
        long now = System.nanoTime();
        if (lastFrameNanos != 0) {
            samples[nextIndex] = now - lastFrameNanos;
            nextIndex = (nextIndex + 1) % CAPACITY;
            sampleCount = Math.min(CAPACITY, sampleCount + 1);
        }
        lastFrameNanos = now;

        if (sampleCount >= 20) {
            LiveStatus.setFps(summarize());
        }
    }

    private static String summarize() {
        long[] copy = Arrays.copyOf(samples, sampleCount);
        Arrays.sort(copy);

        double avgMs = Arrays.stream(copy).average().orElse(0) / 1_000_000.0;
        double avgFps = avgMs > 0 ? 1000.0 / avgMs : 0;
        // Sorted ascending by frame *time*, so the slowest (worst) frames are at the tail -
        // exactly the ones that make up the 1%/0.1% "low" FPS figures.
        double p99Ms = copy[(int) (copy.length * 0.99)] / 1_000_000.0;
        double p999Ms = copy[(int) (copy.length * 0.999 > copy.length - 1 ? copy.length - 1 : copy.length * 0.999)] / 1_000_000.0;
        double low1Fps = p99Ms > 0 ? 1000.0 / p99Ms : 0;
        double low01Fps = p999Ms > 0 ? 1000.0 / p999Ms : 0;

        return String.format(Locale.ROOT, "fps: avg %.0f  |  1%% low %.0f  |  0.1%% low %.0f  |  frame %.1fms",
                avgFps, low1Fps, low01Fps, avgMs);
    }
}
