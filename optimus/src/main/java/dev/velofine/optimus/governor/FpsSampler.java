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

package dev.velofine.optimus.governor;

/**
 * A rolling average of {@code Minecraft.getFps()} over the last few seconds.
 *
 * <p>Two levels of smoothing on purpose. Per-second buckets absorb the single-frame spikes that a
 * chunk rebuild or a GC pause produces, and averaging several buckets stops one bad second from
 * moving render distance. Reacting to an instantaneous frame rate would make the governor thrash
 * on exactly the hardware it is meant to help.
 *
 * <p>Client-thread only, so plain fields - the same reasoning {@code TickProfiler} documents.
 */
public final class FpsSampler {

    private static final int TICKS_PER_SECOND = 20;

    private final int[] buckets;

    private int bucketIndex;
    private int bucketsFilled;
    private long tickFpsSum;
    private int ticksInBucket;

    public FpsSampler(int windowSeconds) {
        this.buckets = new int[Math.max(1, windowSeconds)];
    }

    public void onTick(int fps) {
        tickFpsSum += fps;
        ticksInBucket++;
        if (ticksInBucket < TICKS_PER_SECOND) {
            return;
        }
        buckets[bucketIndex] = (int) (tickFpsSum / ticksInBucket);
        bucketIndex = (bucketIndex + 1) % buckets.length;
        bucketsFilled = Math.min(bucketsFilled + 1, buckets.length);
        tickFpsSum = 0;
        ticksInBucket = 0;
    }

    /** False until a full window has been collected - the governor must not act on partial data. */
    public boolean ready() {
        return bucketsFilled == buckets.length;
    }

    public int average() {
        if (bucketsFilled == 0) {
            return 0;
        }
        long total = 0;
        for (int i = 0; i < bucketsFilled; i++) {
            total += buckets[i];
        }
        return (int) (total / bucketsFilled);
    }

    public void reset() {
        bucketIndex = 0;
        bucketsFilled = 0;
        tickFpsSum = 0;
        ticksInBucket = 0;
    }
}
