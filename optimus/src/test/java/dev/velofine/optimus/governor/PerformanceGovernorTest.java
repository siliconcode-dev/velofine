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

import dev.velofine.core.config.ConfigManager;
import dev.velofine.core.config.GovernorMode;
import dev.velofine.core.config.VelofineConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link GovernorHost} exists precisely so this decision logic - hysteresis, cooldown, a grace
 * period, a ceiling that must never be exceeded - can be driven with a scripted frame-rate
 * sequence and the exact render distances it chooses asserted, per that interface's own class
 * javadoc. This is the only real verification available for logic nobody on this project can test
 * on the actual reference hardware.
 *
 * <p>The grace period (200 ticks) is a private constant, not configurable, so every scenario here
 * ticks through it first regardless of what else it is testing.
 */
final class PerformanceGovernorTest {

    private static final int TICKS_PER_SECOND = 20;
    private static final int GRACE_TICKS = 200;

    private FakeGovernorHost host;
    private VelofineConfig.GovernorSection governor;

    @BeforeEach
    void setUpConfig(@TempDir Path gameDir) {
        ConfigManager.load(gameDir);
        ConfigManager.get().engines.optimus = true;
        governor = ConfigManager.get().optimus.governor;
        governor.mode = GovernorMode.ADAPTIVE;
        governor.checkIntervalSeconds = 1;
        governor.cooldownSeconds = 0;
        governor.decreaseStep = 2;
        governor.increaseStep = 1;
        governor.floorRenderDistance = 4;
        governor.baselineRenderDistance = 0;

        host = new FakeGovernorHost();
        host.renderDistance = 12;
    }

    @Test
    void staysIdleOutsideAWorld() {
        host.inWorld = false;
        PerformanceGovernor governorUnderTest = new PerformanceGovernor(host, 1);

        governorUnderTest.onClientTick();

        assertEquals(12, host.renderDistance, "must never touch render distance while not in a world");
    }

    @Test
    void manualModePinsToTheConfiguredDistanceOnWorldJoin() {
        governor.mode = GovernorMode.MANUAL;
        governor.manualRenderDistance = 6;
        PerformanceGovernor governorUnderTest = new PerformanceGovernor(host, 1);

        governorUnderTest.onClientTick();

        assertEquals(6, host.renderDistance);
    }

    @Test
    void adaptiveDoesNothingDuringTheWorldJoinGracePeriod() {
        host.fps = 10; // well below any plausible minFps
        PerformanceGovernor governorUnderTest = new PerformanceGovernor(host, 1);

        tick(governorUnderTest, GRACE_TICKS - 1);

        assertEquals(12, host.renderDistance);
    }

    @Test
    void descendsWhenAverageFpsFallsBelowTheMinimum() {
        governor.minFps = 45;
        host.fps = 30;
        PerformanceGovernor governorUnderTest = new PerformanceGovernor(host, 1);

        // Grace period, then exactly one full sample window + evaluation interval (both 1s here).
        tick(governorUnderTest, GRACE_TICKS + TICKS_PER_SECOND);

        assertEquals(10, host.renderDistance, "12 - decreaseStep(2) = 10");
    }

    @Test
    void climbsWhenAverageFpsExceedsTheMaximumButNeverPastTheCeiling() {
        governor.maxFps = 75;
        host.fps = 200;
        PerformanceGovernor governorUnderTest = new PerformanceGovernor(host, 1);

        // First evaluation sets the ceiling to the starting distance (12, first-run adoption),
        // then climbs by increaseStep(1) - but climbing is clamped at that same ceiling.
        tick(governorUnderTest, GRACE_TICKS + TICKS_PER_SECOND);

        assertEquals(12, host.renderDistance, "cannot climb past the ceiling adopted from the starting distance");
    }

    @Test
    void neverDescendsBelowTheFloor() {
        governor.minFps = 200; // guarantees every evaluation wants to descend
        governor.decreaseStep = 100; // would go deeply negative without the floor clamp
        host.fps = 1;
        PerformanceGovernor governorUnderTest = new PerformanceGovernor(host, 1);

        tick(governorUnderTest, GRACE_TICKS + TICKS_PER_SECOND);

        assertEquals(governor.floorRenderDistance, host.renderDistance);
    }

    @Test
    void aCooldownSuppressesTheNextChangeUntilItExpires() {
        governor.minFps = 45;
        governor.cooldownSeconds = 5;
        host.fps = 30;
        PerformanceGovernor governorUnderTest = new PerformanceGovernor(host, 1);

        tick(governorUnderTest, GRACE_TICKS + TICKS_PER_SECOND);
        assertEquals(10, host.renderDistance, "first descent should still apply");

        // One more full evaluation interval, still well inside the 5s cooldown.
        tick(governorUnderTest, TICKS_PER_SECOND);
        assertEquals(10, host.renderDistance, "a second descent must be suppressed while on cooldown");
    }

    @Test
    void offModeRestoresTheBaselineRenderDistance() {
        // Get the governor to actually change something first, so releaseControl has a real
        // governor-applied value (not -1) to restore from.
        governor.minFps = 45;
        host.fps = 30;
        PerformanceGovernor governorUnderTest = new PerformanceGovernor(host, 1);
        tick(governorUnderTest, GRACE_TICKS + TICKS_PER_SECOND);
        assertEquals(10, host.renderDistance);

        governor.mode = GovernorMode.OFF;
        governorUnderTest.onClientTick();

        assertEquals(12, host.renderDistance, "should hand render distance back to the user's own baseline");
    }

    @Test
    void aUserChangeWhileAdaptiveIsActiveBecomesTheNewCeiling() {
        governor.minFps = 45;
        governor.maxFps = 75;
        host.fps = 60; // in-band - the governor itself will not touch render distance
        PerformanceGovernor governorUnderTest = new PerformanceGovernor(host, 1);

        tick(governorUnderTest, GRACE_TICKS + TICKS_PER_SECOND);
        assertEquals(12, host.renderDistance, "60fps is between min and max - no change expected");
        assertEquals(12, governor.baselineRenderDistance, "first-run adoption should have set the ceiling");

        // The player raises their own render distance in Video Settings mid-session.
        host.renderDistance = 20;
        tick(governorUnderTest, TICKS_PER_SECOND);

        assertEquals(20, governor.baselineRenderDistance, "the player's own change should become the new ceiling");
    }

    private static void tick(PerformanceGovernor governor, int count) {
        for (int i = 0; i < count; i++) {
            governor.onClientTick();
        }
    }

    private static final class FakeGovernorHost implements GovernorHost {
        boolean inWorld = true;
        boolean screenOpen = false;
        int fps = 60;
        int renderDistance = 12;

        @Override
        public boolean inWorld() {
            return inWorld;
        }

        @Override
        public boolean screenOpen() {
            return screenOpen;
        }

        @Override
        public int fps() {
            return fps;
        }

        @Override
        public int renderDistance() {
            return renderDistance;
        }

        @Override
        public void setRenderDistance(int chunks) {
            renderDistance = chunks;
        }
    }
}
