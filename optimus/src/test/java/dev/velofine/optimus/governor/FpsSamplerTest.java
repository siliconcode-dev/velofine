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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FpsSamplerTest {

    private static final int TICKS_PER_SECOND = 20;

    @Test
    void notReadyUntilTheFullWindowHasBeenCollected() {
        FpsSampler sampler = new FpsSampler(3);
        feedConstantFps(sampler, 60, 2 * TICKS_PER_SECOND);

        assertFalse(sampler.ready());
    }

    @Test
    void readyOnceTheFullWindowHasBeenCollected() {
        FpsSampler sampler = new FpsSampler(3);
        feedConstantFps(sampler, 60, 3 * TICKS_PER_SECOND);

        assertTrue(sampler.ready());
        assertEquals(60, sampler.average());
    }

    @Test
    void averagesAcrossTheWholeWindowNotJustTheLatestBucket() {
        FpsSampler sampler = new FpsSampler(2);
        feedConstantFps(sampler, 40, TICKS_PER_SECOND);
        feedConstantFps(sampler, 60, TICKS_PER_SECOND);

        assertEquals(50, sampler.average());
    }

    @Test
    void oldBucketsAreEvictedAsTheWindowSlides() {
        FpsSampler sampler = new FpsSampler(2);
        feedConstantFps(sampler, 20, TICKS_PER_SECOND);
        feedConstantFps(sampler, 60, TICKS_PER_SECOND);
        feedConstantFps(sampler, 60, TICKS_PER_SECOND);

        // The first (20fps) bucket should have rolled off a 2-bucket window by now.
        assertEquals(60, sampler.average());
    }

    @Test
    void resetClearsAllAccumulatedState() {
        FpsSampler sampler = new FpsSampler(3);
        feedConstantFps(sampler, 60, 3 * TICKS_PER_SECOND);
        assertTrue(sampler.ready());

        sampler.reset();

        assertFalse(sampler.ready());
        assertEquals(0, sampler.average());
    }

    @Test
    void aPartialBucketDoesNotContributeUntilItFills() {
        FpsSampler sampler = new FpsSampler(1);
        feedConstantFps(sampler, 60, TICKS_PER_SECOND - 1);

        assertFalse(sampler.ready());
        assertEquals(0, sampler.average());
    }

    private static void feedConstantFps(FpsSampler sampler, int fps, int tickCount) {
        for (int i = 0; i < tickCount; i++) {
            sampler.onTick(fps);
        }
    }
}
