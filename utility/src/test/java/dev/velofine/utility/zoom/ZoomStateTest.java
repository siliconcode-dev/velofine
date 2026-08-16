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

package dev.velofine.utility.zoom;

import dev.velofine.core.config.ConfigManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ZoomState} drives its smoothing curve off real {@code System.nanoTime()} deltas between
 * calls (see its own class javadoc on why - the frame-rate-vs-tick-rate mismatch that motivated
 * this design), with no injectable clock. That makes exact intermediate values inherently
 * timing-dependent and not something to assert precisely in a deterministic suite; these tests
 * instead cover the parts of its contract that do not depend on wall-clock timing: {@code
 * onScroll}'s applicability gating, and {@code currentFactor()}'s invariant of never going below
 * 1.0 regardless of prior state.
 */
final class ZoomStateTest {

    @BeforeEach
    void setUp(@TempDir Path gameDir) {
        ConfigManager.load(gameDir);
        ZoomState.resetForTest();
    }

    @Test
    void scrollDoesNothingWhileTheZoomKeyIsNotHeld() {
        assertFalse(ZoomState.onScroll(1.0));
    }

    @Test
    void scrollDoesNothingWhenScrollAdjustIsDisabled() {
        ConfigManager.get().utility.zoom.scrollAdjustable = false;
        ZoomState.onTick(true);

        assertFalse(ZoomState.onScroll(1.0));
    }

    @Test
    void scrollDoesNothingWhenZoomItselfIsDisabled() {
        ConfigManager.get().utility.zoom.enabled = false;
        ZoomState.onTick(true);

        assertFalse(ZoomState.onScroll(1.0));
    }

    @Test
    void scrollIsConsumedWhileTheZoomKeyIsHeldAndScrollAdjustIsEnabled() {
        ConfigManager.get().utility.zoom.enabled = true;
        ConfigManager.get().utility.zoom.scrollAdjustable = true;
        ZoomState.onTick(true);

        assertTrue(ZoomState.onScroll(1.0));
    }

    @Test
    void currentFactorNeverGoesBelowOne() {
        ZoomState.onTick(false);
        for (int i = 0; i < 1000; i++) {
            assertTrue(ZoomState.currentFactor() >= 1.0);
        }
    }

    @Test
    void currentFactorNeverGoesBelowOneEvenAfterZoomingIn() {
        ZoomState.onTick(true);
        ZoomState.currentFactor();
        ZoomState.onTick(false);

        for (int i = 0; i < 1000; i++) {
            assertTrue(ZoomState.currentFactor() >= 1.0);
        }
    }
}
