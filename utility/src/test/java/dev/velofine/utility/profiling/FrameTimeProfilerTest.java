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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FrameTimeProfiler} derives frame time from real {@code System.nanoTime()} deltas between
 * calls, so exact avg/1%/0.1% low figures are not deterministically assertable here - this covers
 * the parts of its contract that are: it publishes nothing until it has enough samples, it does
 * publish (in the documented format) once it does, and its fixed-size ring buffer does not break
 * once it wraps past capacity.
 */
final class FrameTimeProfilerTest {

    @BeforeEach
    void resetProfiler() {
        FrameTimeProfiler.resetForTest();
    }

    @Test
    void publishesNothingBeforeTwentySamples() {
        for (int i = 0; i < 19; i++) {
            FrameTimeProfiler.onFrame();
        }
        assertEquals("fps: --", LiveStatus.fps(), "should still be the untouched default below the 20-sample threshold");
    }

    @Test
    void publishesASummaryOnceEnoughSamplesExist() {
        for (int i = 0; i < 25; i++) {
            FrameTimeProfiler.onFrame();
        }
        assertNotEquals("fps: --", LiveStatus.fps());
        assertTrue(LiveStatus.fps().startsWith("FPS"));
        assertTrue(LiveStatus.fps().contains("1% LOW"));
        assertTrue(LiveStatus.fps().contains("0.1% LOW"));
        // v1.5 settings-UI overhaul: the numeric breakdown is now reachable independent of the
        // formatted string too - see FrameStats' class javadoc for why that matters.
        assertTrue(LiveStatus.fpsStats().avgFps() > 0, "structured stats should be published alongside the string");
    }

    @Test
    void survivesWrappingPastRingBufferCapacity() {
        // Capacity is 512 samples - this exercises the wrap-around without asserting exact values.
        for (int i = 0; i < 600; i++) {
            FrameTimeProfiler.onFrame();
        }
        assertTrue(LiveStatus.fps().startsWith("FPS"), "should still produce a well-formed summary past capacity");
    }
}
